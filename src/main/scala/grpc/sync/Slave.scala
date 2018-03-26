package grpc.sync

import computations.SVM
import grpc.sync.SlaveServiceGrpc.SlaveServiceStub
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import utils.Label
import utils.Types.SparseVector

object Slave extends App {

  val instance = this
  val channel = ManagedChannelBuilder
    .forAddress("localhost", 50050) // host and port of service
    .usePlaintext(true) // don't use encryption (for demo purposes)
    .build
  val client: SlaveServiceStub = SlaveServiceGrpc.stub(channel)
  val responseObserver = new StreamObserver[SlaveResponse] {
    def onError(t: Throwable): Unit = {
      println(s"ON_ERROR: $t")
      sys.exit(1)
    }

    def onCompleted(): Unit = {
      println("ON_COMPLETED")
      sys.exit(0)
    }

    def onNext(res: SlaveResponse): Unit = {
      val newGradient = SVM.computeStochasticGradient(
        feature = res.feature,
        label = Label(res.label),
        weights = res.weights,
        lambda = res.lambda,
        tidCounts = res.tidCounts
      )
      //println(s"[CPT]: computing done (gradient = $newGradient)")
      instance.synchronized {
        someGradient = Some(newGradient)
        instance.notify()
      }
    }
  }
  val requestObserver = client.updateWeights(responseObserver)
  var someGradient: Option[SparseVector] = Some(Map.empty)

  println(">> SPAWNED <<")

  while (!channel.isTerminated) {
    instance.synchronized {
      while (someGradient.isEmpty) wait()
      requestObserver.onNext(SlaveRequest(someGradient.get))
      someGradient = None
    }
  }

}
