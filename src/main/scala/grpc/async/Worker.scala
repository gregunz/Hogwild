package  grpc.async

import dataset.Dataset
import grpc.sync.WorkerServiceGrpc.WorkerServiceStub
import grpc.sync._
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import model.{GrpcServer, SVM}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object Worker extends GrpcServer {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val connectionsHandler = new ConnectionsHandler
  var id_to_send = -1

  val instance = this
  var count = 0

  val lambda: Double = 0.1
  val svm = SVM()

  var i = 0
  var j = 0

  var time: Long = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    println("Please enter a worker ID")
    val WorkerId = StdIn.readInt()
    id_to_send = WorkerId
    println(WorkerId)
    load()

    establishCommunications(WorkerId)
  }

  def load(): Unit = {
    Dataset.load()
    samples
  }

  def establishCommunications(WorkerID : Int): Unit = {

    val portNumber = 50050 + WorkerID
    val ssd = WorkerServiceGrpc.bindService(WorkerService, ExecutionContext.global)

    var activeWorker: Boolean = true

    if (WorkerID > 1) {

      // TODO : change this Default ports for the moment
      val portNumbers = List(50050, 50051)

      val channels = portNumbers.map( portNumber =>
        ManagedChannelBuilder
          .forAddress("localhost", portNumber)
          .usePlaintext(true)
          .build
      )
      portNumbers.foreach(connectionsHandler.addWorker("",_))

      val clients: List[WorkerServiceStub] = channels.map(WorkerServiceGrpc.stub(_))

      val HelloMessage = new StreamObserver[WorkerBroadcast] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          sys.exit(1)
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          sys.exit(0)
        }

        def onNext(message: WorkerBroadcast): Unit = {

          count = message.counter + 1
          println(count)
          //println(id_to_send)
          if (message.counter > 300) {
            println(s"Message received")
            println(message.msg, message.myId, message.counter, message.port)
            this.onCompleted()
          }
        }
      }

      val messageObservers = clients.map( client => client.updateWeights(HelloMessage) )

      while (activeWorker) {

        j += 1
        //activeWorker = false
        if (j % 10000 == 0) {
          count += 1
          if (id_to_send != -1) {
            println("Hello accepted : Id changed")
            println(id_to_send)
          }
          messageObservers.foreach(observer => observer.onNext(WorkerBroadcast(id_to_send, "Test", portNumber, count)))
        }
        if(count > 50) {
          return
        }
      }

      println(s"Channel on port $portNumber has been shut down")
    }

    println(">> SERVER READY <<")
    runServer(ssd, portNumber)
  }

  object WorkerService extends WorkerServiceGrpc.WorkerService {

    val instance = this

    override def updateWeights(responseObserver: StreamObserver[WorkerBroadcast]): StreamObserver[WorkerBroadcast] =
      new StreamObserver[WorkerBroadcast] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
        }

        def onNext(message: WorkerBroadcast): Unit = {
          println("------>", message.myId)
          if (message.myId != -1) {
            connectionsHandler.addTest(message.myId)
            if (i % 500 == 0) {
              val loss = connectionsHandler.getTest
              val duration = System.currentTimeMillis() - time
              time = System.currentTimeMillis()
              println(s"[UPT][$i][$duration]: loss = $loss}")
            }
            i += 1
          } else {
            if (!(connectionsHandler.getPorts contains message.port)) {
              println("[NEW]: a new worker has arrived !")
              connectionsHandler.addWorker("",message.port)
            }
          }
          val workerId = message.myId
          println(workerId)
          val content = message.msg
          val port = message.port
          val counter = message.counter
          count = message.counter + 1
          println(s"[MSG $counter] Worker $workerId says : $content")
          responseObserver.onNext(WorkerBroadcast(myId = 0, msg = "My_updates", port = 5005, counter = count))

        }
      }

    override def identification(request: InformationRequest): Future[InformationResponse] = ???

    override def ready(request: Worker_address): Future[Workers_details] = ???
  }
}