package  grpc.async

import java.net._

import dataset.Dataset
import grpc.sync.SlaveServiceGrpc.SlaveServiceStub
import grpc.sync._
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import model.{GrpcServer, SVM}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object Worker2 extends GrpcServer {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val connectionsHandler = new ConnectionsHandler

  val instance = this

  var time: Long = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    println("Please enter a worker ID, 0 or 1")
    val WorkerId = StdIn.readInt()
    load()
    establishCommunications(WorkerId)
  }

  def load(): Unit = {
    Dataset.load()
    samples
  }

  def establishCommunications(WorkerID : Int): Unit = {

    val portNumber = 50050
    val ssd = WorkerServiceGrpc.bindService(WorkerService, ExecutionContext.global)


    // If worker is the operator you start the server right away and wait for other workers to contact you
    if (WorkerID == 0){
      runServer(ssd, portNumber)
    } else {
      // Get local address to send it to the coordinator for port handling
      val localhost: InetAddress = InetAddress.getLocalHost
      val localIpAddress: String = localhost.getHostAddress

      // Send local address to coordinator an get unused port in return
      val channel = ManagedChannelBuilder.forAddress("localhost", portNumber).usePlaintext(true).build
      val blockingStub = WorkerServiceGrpc.blockingStub(channel)
      val id = InformationRequest(localIpAddress)
      val response = blockingStub.identification(id)

      // Run server on port returned
      val serverFuture : Future[Unit] = Future {
        runServer(ssd, response.port)
      }(ExecutionContext.global)

      // Tell coordinator that server is ready and ask for other worker that are ready
      val addresses = blockingStub.ready(Worker_address(localIpAddress, response.port))

      // Drop first entry because it is us
      val other_workers = addresses.items.toList.drop(1)

      // Create a channel for every worker that as a running server
      val channels = other_workers.map(x => ManagedChannelBuilder
        .forAddress(x.address, x.port)
        .usePlaintext(true)
        .build)

      // TODO : OPEN CHANNEL AND START WORKING

    }
  }

  object WorkerService extends WorkerServiceGrpc.WorkerService {

    // Stores addresses and ports of running servers
    var current_workers_addresses = List[Worker_address]()

    // Keep count of addresses and ports for when a new worker asks for a port
    var workers_addresses_taken = Map[String, Int]()
    var counter = 0

    override def updateWeights(responseObserver: StreamObserver[WorkerBroadcast]): StreamObserver[WorkerBroadcast] = ???

    override def identification(request: InformationRequest): Future[InformationResponse] = {

      var port = 50051
      if (workers_addresses_taken.contains(request.address)){
        val count = workers_addresses_taken(request.address)
        workers_addresses_taken = workers_addresses_taken + (request.address -> (count + 1))
        port += count
      } else {
        workers_addresses_taken = workers_addresses_taken + (request.address -> 1)
      }

      Future.successful(InformationResponse(port))
    }

    override def ready(request: Worker_address): Future[Workers_details] = {
      current_workers_addresses = request::current_workers_addresses
      Future.successful(Workers_details(current_workers_addresses))
    }
  }

  def getListOfWorkers(map: Map[String, Int]): List[(Any, Int)] = {
    (map.view.map{ case(k,v) => (k, v) } toList).flatMap{case(c, i) => (0 until i).map((c, _)) }
  }
}