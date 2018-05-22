package grpc.async

import java.net._

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import io.grpc.{Context, ManagedChannel, ManagedChannelBuilder}
import model._
import utils.AsyncWorkerMode

import scala.concurrent.{ExecutionContext, Future}

object Worker extends GrpcServer with GrpcRunnable[AsyncWorkerMode] {

  type BlockingStub = WorkerServiceAsyncGrpc.WorkerServiceAsyncBlockingStub
  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  type Broadcaster = StreamObserver[BroadcastMessage]

  private val broadcastersHandler = BroadcastersHandler

  /* TO COMPUTE & PRINT LOSSES */
  lazy val (someFeatures, someLabels) = Dataset.getSubset(subsetSize).unzip
  val subsetSize = 500
  var time: Long = System.currentTimeMillis()

  def run(mode: AsyncWorkerMode): Unit = {
    load()
    val svm = new SVM()
    val myIp: String = InetAddress.getLocalHost.getHostAddress
    val myPort = mode.port
    val weightsUpdateHandler: WeightsUpdateHandler = WeightsUpdateHandler(mode.interval)

    startServer(myIp, myPort, svm)

    if (mode.worker.isEmpty) {
      startComputations(myIp, myPort, svm, weightsUpdateHandler)
    } else {
      val worker = mode.worker.get
      val channel = createChannel(worker)
      val (weights, workers) = hello(myIp, myPort, worker, channel)
      svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights

      broadcastersHandler.add(worker, channel)
      broadcastersHandler.add(workers.filterNot(_ == worker))
      startComputations(myIp, myPort, svm, weightsUpdateHandler)
    }
  }

  def load(): Unit = {
    Dataset.fullLoad()
    someFeatures
    someLabels
  }

  def createChannel(worker: RemoteWorker):ManagedChannel  = {
    ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build
  }

  def hello(myIp: String, myPort: Int, worker: RemoteWorker, channel: ManagedChannel): (SparseNumVector[Double], Set[RemoteWorker]) = {
    val stub = WorkerServiceAsyncGrpc.blockingStub(channel)
    val response = stub.hello(WorkerDetail(myIp, myPort))

    SparseNumVector(response.weights) -> response
      .workersDetails
      .map(a => RemoteWorker(a.address, a.port.toInt))
      .toSet
  }

  def startServer(myIp: String, myPort: Int, svm: SVM): Unit = {
      val ssd = WorkerServiceAsyncGrpc.bindService(WorkerServerService(myIp, myPort, svm), ExecutionContext.global)
      println(">> Server starting..")
      runServer(ssd, myPort)
  }

  def startComputations(myIp: String, myPort: Int, svm: SVM, weightsHandler: WeightsUpdateHandler): Unit = {
    val myWorkerDetail = WorkerDetail(myIp, myPort)
    val samples: Iterator[Int] = Dataset.samples().toIterator

    println(">> Computations thread starting..")

    while (true) {
      val random_did = samples.next
      val newGradient = svm.computeStochasticGradient(
        feature = Dataset.getFeature(random_did),
        label = Dataset.getLabel(random_did),
        tidCounts = Dataset.tidCounts
      )
      val weightsUpdate = svm.updateWeights(newGradient)
      weightsHandler.addWeightsUpdate(weightsUpdate)

      // here we broadcast the weights update
      if (weightsHandler.shouldBroadcast) {
        val weigthsUpdateToBroadcast = weightsHandler.getAndResetWeightsUpdate()
        val msg = BroadcastMessage(
          weigthsUpdateToBroadcast.toMap,
          Some(myWorkerDetail)
        )
        if (broadcastersHandler.hasBroadcaster) {
          println(s"[SEND] feel like sharing some computations, here you go guys " +
            s"(${broadcastersHandler.workers.mkString("[", ";", "]")})")

          broadcastersHandler.broadcast(msg)
        }

        // compute loss
        val loss = svm.loss(
          someFeatures,
          someLabels,
          Dataset.tidCounts
        )
        val duration = System.currentTimeMillis() - time
        time = System.currentTimeMillis()
        println(s"[UPT][$duration]: loss = $loss")

      }
    }
  }

  case class RemoteWorker(ip: String, port: Int)

  case class WorkerServerService(myIp: String, myPort: Int, svm: SVM) extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    override def hello(request: WorkerDetail): Future[HelloResponse] = {
      val workersToSend = broadcastersHandler.workers + RemoteWorker(myIp, myPort)
      val weights = svm.weights

      broadcastersHandler.add(RemoteWorker(request.address, request.port.toInt))

      Future.successful(HelloResponse(
        workersToSend.map { w => WorkerDetail(w.ip, w.port) }.toSeq,
        weights.toMap
      ))
    }

    override def broadcast(responseObserver: StreamObserver[Empty]): StreamObserver[BroadcastMessage] = {
      new StreamObserver[BroadcastMessage] {
        override def onError(t: Throwable): Unit = {}

        override def onCompleted(): Unit = {}

        override def onNext(msg: BroadcastMessage): Unit = {
          val detail = msg.workerDetail.get
          val worker = RemoteWorker(detail.address, detail.port.toInt)

          broadcastersHandler.add(worker)
          println(s"[RECEIVED]: thanks to $worker for the computation, I owe you some gradients now ;)")

          val receivedWeights = SparseNumVector(msg.weightsUpdate)
          svm.addWeightsUpdate(receivedWeights)
        }
      }
    }
  }

}