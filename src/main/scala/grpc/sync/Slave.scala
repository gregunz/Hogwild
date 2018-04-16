package grpc.sync

import dataset.Dataset
import grpc.sync.SlaveServiceGrpc.SlaveServiceStub
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import model.{SVM, SparseNumVector}

object Slave extends App {

  val lambda = 0.1
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
        feature = Dataset.getFeature(res.did),
        label = Dataset.getLabel(res.did),
        weights = SparseNumVector(res.weights),
        lambda = lambda,
        tidCounts = Dataset.tidCounts
      )
      count += 1
      if (count % 1000 == 0) {
        println(s"[CPT]: computing done since start = $count)")
      }

      instance.synchronized {
        someGradient = Some(newGradient)
        instance.notifyAll()
      }
    }
  }
  val requestObserver = client.updateWeights(responseObserver)
  var someGradient: Option[SparseNumVector] = Some(SparseNumVector.empty)
  // FOR PRINTS
  var count = 0

  println(">> SPAWNED <<")

  while (!channel.isTerminated) {
    instance.synchronized {
      while (someGradient.isEmpty) instance.wait()
      requestObserver.onNext(SlaveRequest(someGradient.get.values))
      someGradient = None
    }
  }

}
