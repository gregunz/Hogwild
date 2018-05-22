package grpc.async

import java.net._

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import model._
import utils.AsyncWorkerMode

import scala.concurrent.{ExecutionContext, Future}

object Worker extends GrpcServer with GrpcRunnable[AsyncWorkerMode] {

  type BlockingStub = WorkerServiceAsyncGrpc.WorkerServiceAsyncBlockingStub
  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  case class RemoteWorker(ip: String, port: Int)
  type Broadcaster = StreamObserver[BroadcastMessage]

  val broadcastersHandler = BroadcastersHandler

  /* TO COMPUTE & PRINT LOSSES */
  val subsetSize = 500
  lazy val (someFeatures, someLabels) = Dataset.getSubset(subsetSize).unzip
  var time: Long = System.currentTimeMillis()

  def run(mode: AsyncWorkerMode): Unit = {
    val svm = new SVM()
    val myIp: String = InetAddress.getLocalHost.getHostAddress
    val weightsUpdateHandler: WeightsUpdateHandler = WeightsUpdateHandler(mode.interval)

    if (mode.worker.isEmpty) {
      start(myIp, mode.port, svm, weightsUpdateHandler)
    } else {
      val worker = mode.worker.get
      val stub = createBlockingStub(worker)
      val (weights, workers) = hello(stub)
      svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights
      broadcastersHandler.add(workers)
      start(myIp, mode.port, svm, weightsUpdateHandler)
    }
  }

  def load(): Unit = {
    someFeatures
    someLabels
  }

  def start(myIp: String, myPort: Int, svm: SVM, weightsUpdateHandler: WeightsUpdateHandler): Unit = {
    load()
    startWorkingThread(myIp, myPort, svm, weightsUpdateHandler)
    startServer(myIp, myPort, svm)
  }

  def createBlockingStub(worker: RemoteWorker): BlockingStub = {
    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build

    WorkerServiceAsyncGrpc.blockingStub(channel)
  }

  def hello(oneWorkerStub: BlockingStub): (SparseNumVector[Double], Set[RemoteWorker]) = {
    val response = oneWorkerStub.hello(Empty())
    SparseNumVector(response.weights) -> response
      .workersDetails
      .map(a => RemoteWorker(a.address, a.port.toInt))
      .toSet
  }

  def startServer(myIp: String, myPort: Int, svm: SVM): Unit = {
    println(">> Server starting..")
    val ssd = WorkerServiceAsyncGrpc.bindService(WorkerServerService(myIp, myPort, svm), ExecutionContext.global)
    println(">> Ready!")
    runServer(ssd, myPort)
  }

  def startWorkingThread(myIp: String, myPort: Int, svm: SVM, weightsHandler: WeightsUpdateHandler): Future[Unit] = {
    val myWorkerDetail = WorkerDetail(myIp, myPort)
    val samples: Iterator[Int] = Dataset.samples().toIterator

    println(">> Computations thread set up..")

    Future {
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
          if (broadcastersHandler.hasBroadcaster){
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

    }(ExecutionContext.global)
  }

  case class WorkerServerService(myIp: String, myPort: Int, svm: SVM) extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    override def hello(request: Empty): Future[HelloResponse] = {
      val workersToSend = broadcastersHandler.workers + RemoteWorker(myIp, myPort)
      val weights = svm.weights

      Future.successful(HelloResponse(
        workersToSend.map { w => WorkerDetail(w.ip, w.port) }.toSeq,
        weights.toMap
      ))
    }

    override def broadcast(responseObserver: StreamObserver[Empty]): StreamObserver[BroadcastMessage] = {
      new StreamObserver[BroadcastMessage] {
        override def onError(t: Throwable): Unit = println(s"ON_ERROR: $t")

        override def onCompleted(): Unit = println("ON_COMPLETED")

        override def onNext(msg: BroadcastMessage): Unit = {
          val detail = msg.workerDetail.get
          val worker = RemoteWorker(detail.address, detail.port.toInt)

          println(s"[RECEIVED]: thanks to $worker for the computation, I owe you some gradients now ;)")
          broadcastersHandler.add(worker)

          val receivedWeights = SparseNumVector(msg.weightsUpdate)
          svm.addWeightsUpdate(receivedWeights)
        }
      }
    }
  }

}