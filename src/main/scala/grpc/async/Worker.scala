package grpc.async

import java.net._

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import launcher.mode.AsyncWorkerMode
import model._
import utils.Logger
import utils.Types.TID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Worker extends GrpcServer with GrpcRunnable[AsyncWorkerMode] {

  private var keepComputing = true

  def run(mode: AsyncWorkerMode): Unit = {

    val dataset = mode.dataset.getReady(mode.isMaster)
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val myIp: String = InetAddress.getLocalHost.getHostAddress
    val meWorker = RemoteWorker(myIp, mode.port, mode.name)

    val broadcastersHandler: BroadcastersHandler = Try {
      val (weights, workers) = hello(meWorker, mode.workerIp, mode.workerPort)
      val broadcastersHandler = BroadcastersHandler(mode.logger, dataset, meWorker, mode.broadcastInterval)
      svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights
      broadcastersHandler.addSomeActive(workers)
      broadcastersHandler
    }.getOrElse({
      if (mode.isSlave) {
        throw new IllegalStateException(s"Failed to connect to ${mode.workerIp}:${mode.workerPort}")
      }
      BroadcastersHandler(mode.logger, mode.dataset, meWorker, mode.broadcastInterval)
    })

    mode.logger.log(2)(s" ${if (mode.isMaster) "Master" else "Slave"} $meWorker ready")

    startServer(mode.logger, svm, broadcastersHandler)
    startComputations(mode.logger, dataset, svm, broadcastersHandler, mode.stoppingCriteria)

  }

  def hello(meWorker: RemoteWorker, workerIp: String, workerPort: Int): (SparseNumVector[Double], Set[RemoteWorker]) = {
    val channel = createChannel(workerIp, workerPort)
    val stub = WorkerServiceAsyncGrpc.blockingStub(channel)
    val response = stub.hello(meWorker.toWorkerDetail)
    val weights = SparseNumVector(response.weights)
    val workers = response
      .workersDetails
      .map(RemoteWorker.fromWorkerDetails)
      .toSet

    (weights, workers)
  }

  private def createChannel(ip: String, port: Int): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(ip, port)
      .usePlaintext(true)
      .build
  }

  def startServer(logger: Logger, svm: SVM, broadcastersHandler: BroadcastersHandler): Unit = {
    val ssd = WorkerServiceAsyncGrpc.bindService(WorkerServerService(logger, svm, broadcastersHandler), ExecutionContext.global)
    logger.log(2)("Server starting...")
    runServer(ssd, broadcastersHandler.meWorker.port)
    logger.log(2)("Server ready!")
  }

  def startComputations(logger: Logger, dataset: Dataset, svm: SVM, broadcastersHandler: BroadcastersHandler,
                        someStoppingCriteria: Option[StoppingCriteria]): Unit = {


    logger.log(2)("Computations thread starting...")

    var broadcastFuture = Future.successful()
    var lossComputingFuture = Future.successful()

    while (keepComputing && someStoppingCriteria.forall(!_.shouldStop)) {
      val (feature, label) = dataset.getSample
      val newGradient = svm.computeStochasticGradient(
        feature = feature,
        label = label,
        inverseTidCountsVector = dataset.inverseTidCountsVector
      )
      val weightsUpdate = svm.updateWeights(newGradient)
      WeightsUpdateHandler.addWeightsUpdate(weightsUpdate)

      if (broadcastersHandler.broadcastInterval.hasReachedOrKeepGoing && broadcastFuture.isCompleted) {
        broadcastFuture = Future {
          val weights = WeightsUpdateHandler.getAndResetWeightsUpdate()
          broadcastersHandler.broadcast(weights)
        }
      }
      someStoppingCriteria.foreach { stoppingCriteria =>
        if (stoppingCriteria.interval.hasReachedOrKeepGoing && lossComputingFuture.isCompleted) {
          lossComputingFuture = Future {
            stoppingCriteria.compute(svm, displayLoss = true)
          }
        }
      }
    }

    if (someStoppingCriteria.isDefined) {
      broadcastersHandler.killAll()
      someStoppingCriteria.get.export()
    }

    Thread.sleep(Long.MaxValue)
  }

  def tidsToBroadcast(dataset: Dataset, i: Int, n: Int): Set[TID] = {
    val allTids = dataset.inverseTidCountsVector.keys.toSeq.sorted
    allTids.filter(tid => tid % n == i).toSet
  }

  case class WorkerServerService(logger: Logger, svm: SVM, broadcastersHandler: BroadcastersHandler)
    extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    override def hello(request: WorkerDetail): Future[HelloResponse] = {
      val workersToSend = broadcastersHandler.allWorkers
      val newWorker = RemoteWorker.fromWorkerDetails(request)
      val weights = svm.weights

      broadcastersHandler.addToWaitingList(newWorker)

      Future(HelloResponse(
        workersToSend.toSeq.map(_.toWorkerDetail),
        weights.toMap
      ))
    }

    override def broadcast(responseObserver: StreamObserver[Empty]): StreamObserver[BroadcastMessage] = {
      new StreamObserver[BroadcastMessage] {
        override def onError(t: Throwable): Unit = {}

        override def onCompleted(): Unit = {}

        override def onNext(msg: BroadcastMessage): Unit = {
          val worker = RemoteWorker.fromWorkerDetails(msg.workerDetail.get)

          broadcastersHandler.add(worker)
          logger.log(3)(s"[RECEIVED]: thanks to $worker for the computation, I owe you some gradients now ;)")

          val receivedWeights = SparseNumVector(msg.weightsUpdate)
          svm.addWeightsUpdate(receivedWeights)
        }
      }
    }

    override def kill(request: Empty): Future[Empty] = {
      logger.log(2)(s"[KILLED]: this is the end, my friend... i am proud to have served you... arrrrghhh... (dying alone on the field)")
      keepComputing = false
      //sys.exit(0)
      Future(Empty())
    }
  }

}