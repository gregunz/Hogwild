package grpc.async

import computations.SVM
import computations.SVM.Gradient
import grpc.async.SlaveServiceGrpc.SlaveServiceStub
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

object Slave extends App {

  val instance = this
  var someGradient: Option[Gradient] = Some(Map.empty)

  val channel = ManagedChannelBuilder
    .forAddress("localhost", 50050) // host and port of service
    .usePlaintext(true) // don't use encryption (for demo purposes)
    .build

  val client: SlaveServiceStub = SlaveServiceGrpc.stub(channel)

  val responseObserver = new StreamObserver[SlaveResponse] {
    def onError(t: Throwable): Unit = println(s"ON_ERROR: $t")

    def onCompleted(): Unit = println("ON_COMPLETED")

    def onNext(res: SlaveResponse): Unit = {
      val newGradient = SVM.computeGradient(
        features = res.features,
        label = res.label,
        weights = res.weights
      )
      println(s"[CPT]: computing done (gradient = $newGradient)")
      instance.synchronized {
        someGradient = Some(newGradient)
        instance.notify()
      }
    }
  }

  val requestObserver = client.updateWeights(responseObserver)

  println(">> SPAWNED <<")

  while (!channel.isTerminated) {
    instance.synchronized {
      while (someGradient.isEmpty) wait()
      requestObserver.onNext(SlaveRequest(someGradient.get))
      someGradient = None
    }
  }

}
