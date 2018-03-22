package grpc.async

import computations.SVM
import computations.SVM.Weights
import dataset.Dataset
import io.grpc.stub.StreamObserver

import scala.concurrent.ExecutionContext

object Master extends GrpcServer {

  private val instance = this

  val svm = SVM()

  def main(args: Array[String]): Unit = {
    val ssd = SlaveServiceGrpc.bindService(SlaveService, ExecutionContext.global)
    println("Loading...")
    Dataset.load
    println(">> READY <<")
    runServer(ssd)
  }

  object SlaveService extends SlaveServiceGrpc.SlaveService {

    private def spawnSlaveResponse(weights: Weights): SlaveResponse = {
      val (features, label) = Dataset.sample()
      SlaveResponse(features = features, label = label, weights = weights)
    }

    override def updateWeights(responseObserver: StreamObserver[SlaveResponse]): StreamObserver[SlaveRequest] =
      new StreamObserver[SlaveRequest] {
        def onError(t: Throwable): Unit = println(s"ON_ERROR: $t")

        def onCompleted(): Unit = println("ON_COMPLETED")

        def onNext(req: SlaveRequest): Unit = {
          if (req.gradient.nonEmpty) {
            instance.synchronized(
              svm.updateWeight(req.gradient)
            )
            println(s"[UPT]: new weights = ${svm.weights}}")
          } else {
            println("[NEW]: a slave wants to compute some gradients")
          }
          responseObserver.onNext(spawnSlaveResponse(svm.weights))
        }
      }
  }

}
