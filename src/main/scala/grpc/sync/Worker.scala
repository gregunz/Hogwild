package grpc.sync

import dataset.Dataset
import grpc.GrpcRunnable
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import launcher.SyncWorkerMode
import model.{SVM, SparseNumVector}
import utils.Interval

object Worker extends GrpcRunnable[SyncWorkerMode] {

  private val instance = this
  private var someGradient: Option[SparseNumVector[Double]] = Some(SparseNumVector.empty)

  def run(mode: SyncWorkerMode): Unit = {
    val dataset = Dataset(mode.dataPath).getReady(mode.isMaster)
    val channel = createChannel(mode.serverIp, mode.serverPort)
    val client = WorkerServiceSyncGrpc.stub(channel)
    val responseObserver = createObserver(dataset, mode.lambda, mode.interval)
    val requestObserver = client.updateWeights(responseObserver)

    channel.shutdown()
    println(">> Ready to compute!")
    startComputingLoop(requestObserver)
  }

  def createChannel(ip: String, port: Int): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(ip, port)
      .usePlaintext(true)
      .build
  }

  def createObserver(dataset: Dataset, lambda: Double, interval: Interval): StreamObserver[WorkerResponse] = {
    new StreamObserver[WorkerResponse] {
      def onError(t: Throwable): Unit = {
        println(s"ON_ERROR: $t")
        sys.exit(1)
      }

      def onCompleted(): Unit = {
        println("ON_COMPLETED")
        sys.exit(0)
      }

      def onNext(res: WorkerResponse): Unit = {
        if (res.weightsUpdate.isEmpty) {
          println(s"[KILLED]: this is the end, my friend... i am proud to have served you... arrrrghhh... (dying alone on the field)")
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
        if (interval.resetIfReachedElseIncrease()) {
          println(s"[CPT]: hardworking since ${interval.prettyLimit}")
        }

        instance.synchronized {
          someGradient = Some(newGradient)
          instance.notifyAll()
        }
      }
    }
  }

  def startComputingLoop(requestObserver: StreamObserver[WorkerRequest]): Unit = {
    while (true) {
      instance.synchronized {
        while (someGradient.isEmpty) instance.wait()
        requestObserver.onNext(WorkerRequest(someGradient.get.toMap))
        someGradient = None
      }
    }
  }

}