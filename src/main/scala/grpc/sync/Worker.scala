package grpc.sync

import dataset.Dataset
import grpc.sync.Coordinator.argMismatch
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import launcher.GrpcRunnable
import model.{SVM, SparseNumVector}

object Worker extends GrpcRunnable {

  private val instance = this

  val lambda = 0.1
  var count = 0
  var someGradient: Option[SparseNumVector] = Some(SparseNumVector.empty)

  def run(args: Seq[String]): Unit = {
    args match {
      case port :: _ =>
        val client = createClient(port.toInt)
        val responseObserver = createObserver
        val requestObserver = client.updateWeights(responseObserver)

        println(">> SPAWNED <<")
        startComputingLoop(requestObserver)

      case _ => argMismatch(s"expecting port but get $args")
    }
  }

  def createClient(port: Int): WorkerServiceSyncGrpc.WorkerServiceSyncStub = {
    val channel = ManagedChannelBuilder
      .forAddress("localhost", port) // host and port of service
      .usePlaintext(true) // don't use encryption (for demo purposes)
      .build

    WorkerServiceSyncGrpc.stub(channel)
  }

  def createObserver: StreamObserver[WorkerResponse] = {
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
        val newGradient = SVM.computeStochasticGradient(
          feature = Dataset.getFeature(res.did),
          label = Dataset.getLabel(res.did),
          weights = SparseNumVector(res.weights),
          lambda = lambda,
          tidCounts = Dataset.tidCounts
        )
        count += 1
        if (count % 500 == 0) {
          println(s"[CPT]: computing done since start = $count)")
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
        requestObserver.onNext(WorkerRequest(someGradient.get.values))
        someGradient = None
      }
    }
  }

}