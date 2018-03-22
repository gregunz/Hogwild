package grpc.async

import java.util.concurrent.TimeUnit

import computations.SVM
import computations.SVM.{Features, Label, Weights}
import dataset.Dataset
import io.grpc.stub.StreamObserver

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

object Master extends GrpcServer {

  private val instance = this

  val svm = SVM()
  lazy val samples: Iterator[(Features, Label)] = Dataset.samples().toIterator

  def load(): Unit = {
    val tryLoading = Try(Await.ready(Dataset.load(), Duration.create(1, TimeUnit.MINUTES)))
    if (tryLoading.isFailure) {
      println("Dataset loading failed!!")
      throw tryLoading.failed.get
    }
    samples
  }

  def main(args: Array[String]): Unit = {

    println("Loading...")
    load()
    val ssd = SlaveServiceGrpc.bindService(SlaveService, ExecutionContext.global)

    println(">> READY <<")
    runServer(ssd)
  }

  object SlaveService extends SlaveServiceGrpc.SlaveService {

    private def spawnSlaveResponse(weights: Weights): SlaveResponse = {
      val (features, label) = samples.next
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
