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
  private var first = true
  private var someGradient: Option[SparseNumVector[Double]] = Some(SparseNumVector.empty)

  def run(mode: SyncWorkerMode): Unit = {

    Random.setSeed(mode.seed)

    val dataset = mode.dataset.getReady(mode.isMaster)
    val channel = createChannel(mode.serverIp, mode.serverPort)
    val client = WorkerServiceSyncGrpc.stub(channel)
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val responseObserver = createObserver(mode.logger, dataset, svm)
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

  def createObserver(logger: Logger, dataset: Dataset, svm: SVM): StreamObserver[WorkerResponse] = {
    new StreamObserver[WorkerResponse] {
      val err = "[KILLED] this is the end, my friend... i am proud to have served you... arrrrghhh... (dying alone on the field)"
      def onError(t: Throwable): Unit = {
        t.printStackTrace()
        logger.log(2)(s"$err (on error)")
        sys.exit(0)
      }

      def onCompleted(): Unit = {
        logger.log(2)(s"$err (on completed)")
        sys.exit(0)
      }

      def onNext(res: WorkerResponse): Unit = {
        if (res.stop) {
          logger.log(2)(s"$err")
          sys.exit(0)
        }
        svm.addWeightsUpdate(SparseNumVector(res.weightsUpdate))
        val (feature, label) = dataset.getSample
        val newGradient = svm.computeStochasticGradient(
          feature = feature,
          label = label,
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
        if(first){
          first = false
          logger.log(2)("[HI] i wanna help, give me something to compute!")
        } else {
          //logger.log(3)("[SEND] jobs done, here you go my master!")
        }
        someGradient = None
      }
    }
  }

}