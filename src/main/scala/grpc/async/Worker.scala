package grpc.async

import java.net._

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer, WeightsUpdateHandler}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import model._
import utils.AsyncWorkerMode

import scala.concurrent.{ExecutionContext, Future}

object Worker extends GrpcServer with GrpcRunnable[AsyncWorkerMode] {

  type BlockingStub = WorkerServiceAsyncGrpc.WorkerServiceAsyncBlockingStub
  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  type Broadcaster = StreamObserver[BroadcastMessage]

  private val broadcastersHandler = BroadcastersHandler


  def run(mode: AsyncWorkerMode): Unit = {

    val dataset = Dataset(mode.dataPath, mode.samples).fullLoad()
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val myIp: String = InetAddress.getLocalHost.getHostAddress
    val myPort = mode.port
    val weightsUpdateHandler: WeightsUpdateHandler = WeightsUpdateHandler(mode.interval, dataset)

    startServer(myIp, myPort, svm)

    if (mode.worker.isDefined) {
      val worker = mode.worker.get
      val channel = createChannel(worker)
      val (weights, workers) = hello(myIp, myPort, worker, channel)
      svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights

      broadcastersHandler.addSomeActive(workers)
    }

    startComputations(myIp, myPort, dataset, svm, weightsUpdateHandler)
  }

  def createChannel(worker: RemoteWorker): ManagedChannel = {
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

  def startComputations(myIp: String, myPort: Int, dataset: Dataset, svm: SVM, weightsHandler: WeightsUpdateHandler): Unit = {
    val myWorkerDetail = WorkerDetail(myIp, myPort)
    val samples: Iterator[Int] = dataset.samples().toIterator



    println(">> Computations thread starting..")

    while (true) {
      val random_did = samples.next
      val newGradient = svm.computeStochasticGradient(
        feature = dataset.getFeature(random_did),
        label = dataset.getLabel(random_did),
        tidCounts = dataset.tidCounts
      )
      val weightsUpdate = svm.updateWeights(newGradient)
      weightsHandler.addWeightsUpdate(weightsUpdate)

      // here we broadcast the weights update
      if (weightsHandler.reachedInterval) {
        val msg = BroadcastMessage(
          weightsHandler.getAndResetWeightsUpdate().toMap,
          Some(myWorkerDetail)
        )
        weightsHandler.resetInterval()

        weightsHandler.showLoss(svm)
        broadcastersHandler.broadcast(msg)
      }
    }
  }

  case class RemoteWorker(ip: String, port: Int)

  case class WorkerServerService(myIp: String, myPort: Int, svm: SVM) extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    override def hello(request: WorkerDetail): Future[HelloResponse] = {
      val workersToSend = broadcastersHandler.activeWorkers + RemoteWorker(myIp, myPort)
      val weights = svm.weights

      broadcastersHandler.addToWaitingList(RemoteWorker(request.address, request.port.toInt))

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