package grpc.sync

import java.util.concurrent.TimeUnit

import computations.Label.Label
import computations.{Label, SVM}
import computations.SVM.{Counts, Feature, Weights}
import dataset.Dataset
import io.grpc.stub.StreamObserver

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

object Master extends GrpcServer {

  private val instance = this

  val lambda: Double = 0.1

  val svm = SVM()
  lazy val samples: Iterator[(Feature, Label, Counts)] = Dataset.samples().toIterator

  def load(): Unit = {
    val tryLoading = Try(Await.ready(Dataset.load(), Duration.create(10, TimeUnit.MINUTES)))
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
      val (features, label, tidCounts) = samples.next
      SlaveResponse(
        features = features,
        label = label == Label.CCAT,
        weights = features.map{case (k, _) => k -> weights.withDefaultValue(0d)(k)},
        lambda = lambda,
        tidCounts = tidCounts
      )
    }

    override def updateWeights(responseObserver: StreamObserver[SlaveResponse]): StreamObserver[SlaveRequest] =
      new StreamObserver[SlaveRequest] {
        def onError(t: Throwable): Unit = println(s"ON_ERROR: $t")

        def onCompleted(): Unit = println("ON_COMPLETED")

        def onNext(req: SlaveRequest): Unit = {
          instance.synchronized {
            if (req.gradient.nonEmpty) {
              svm.updateWeight(req.gradient)
              println(s"[UPT]: new weights = ${svm.weights}}")
            } else {
              println("[NEW]: a slave wants to compute some gradients")
            }
            responseObserver.onNext(spawnSlaveResponse(svm.weights))
          }
        }
      }
  }

}
