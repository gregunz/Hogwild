package grpc.sync

import dataset.Dataset
import grpc.GrpcRunnable
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import launcher.mode.SyncWorkerMode
import model.{SVM, SparseNumVector}
import utils.Logger

import scala.util.Random

object Worker extends GrpcRunnable[SyncWorkerMode] {

  private val instance = this
  private var someGradient: Option[SparseNumVector[Double]] = Some(SparseNumVector.empty)

  def run(mode: SyncWorkerMode): Unit = {

    Random.setSeed(mode.seed)

    val dataset = mode.dataset.getReady(mode.isMaster)
    val channel = createChannel(mode.serverIp, mode.serverPort)
    val client = WorkerServiceSyncGrpc.stub(channel)
    val responseObserver = createObserver(mode.logger, dataset, mode.lambda)
    val requestObserver = client.updateWeights(responseObserver)

    Thread.sleep(10 * 1000)
    mode.logger.log(2)("Ready to compute!")
    startComputingLoop(mode.logger, requestObserver)
  }

  def createChannel(ip: String, port: Int): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(ip, port)
      .usePlaintext(true)
      .build
  }

  def createObserver(logger: Logger, dataset: Dataset, lambda: Double): StreamObserver[WorkerResponse] = {
    new StreamObserver[WorkerResponse] {
      val err = "[KILLED]: this is the end, my friend... i am proud to have served you... arrrrghhh... (dying alone on the field)"
      def onError(t: Throwable): Unit = {
        logger.log(2)(s"$err (on error)")
        sys.exit(0)
      }

      def onCompleted(): Unit = {
        logger.log(2)(s"$err (on completed)")
        sys.exit(0)
      }

      def onNext(res: WorkerResponse): Unit = {
        if (res.weightsUpdate.isEmpty) {
          logger.log(2)(s"$err")
          sys.exit(0)
        }
        val (feature, label) = dataset.getSample
        val newGradient = SVM.computeStochasticGradient(
          feature = feature,
          label = label,
          weights = SparseNumVector(res.weightsUpdate),
          lambda = lambda,
          inverseTidCountsVector = dataset.inverseTidCountsVector
        )

        instance.synchronized {
          someGradient = Some(newGradient)
          instance.notifyAll()
        }
      }
    }
  }

  def startComputingLoop(logger: Logger, requestObserver: StreamObserver[WorkerRequest]): Unit = {
    while (true) {
      instance.synchronized {
        while (someGradient.isEmpty) {instance.wait()}
        requestObserver.onNext(WorkerRequest(someGradient.get.toMap))
        logger.log(3)("[SEND] jobs done, here you go my master!")
        someGradient = None
      }
    }
  }

}