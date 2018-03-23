package grpc.sync

import java.util.concurrent.TimeUnit

import computations.SVM
import dataset.Dataset
import io.grpc.stub.StreamObserver
import util.Label
import util.Label.Label
import util.Types.{Counts, SparseVector}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

object Master extends GrpcServer {

  lazy val samples: Iterator[(SparseVector, Label, Counts)] = Dataset.samples().toIterator
  val lambda: Double = 0.1

  val svm = SVM()
  private val instance = this

  def main(args: Array[String]): Unit = {

    println("Loading...")
    load()
    val ssd = SlaveServiceGrpc.bindService(SlaveService, ExecutionContext.global)

    println(">> READY <<")
    runServer(ssd)
  }

  def load(): Unit = {
    val tryLoading = Try(Await.ready(Dataset.load(), Duration.create(10, TimeUnit.MINUTES)))
    if (tryLoading.isFailure) {
      println("Dataset loading failed!!")
      throw tryLoading.failed.get
    }
    samples
  }

  object SlaveService extends SlaveServiceGrpc.SlaveService {

    override def updateWeights(responseObserver: StreamObserver[SlaveResponse]): StreamObserver[SlaveRequest] =
      new StreamObserver[SlaveRequest] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          sys.exit(1)
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          sys.exit(0)
        }

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

    private def spawnSlaveResponse(weights: SparseVector): SlaveResponse = {
      val (feature, label, tidCounts) = samples.next
      SlaveResponse(
        feature = feature,
        label = label == Label.CCAT,
        weights = feature.map { case (k, _) => k -> weights.withDefaultValue(0d)(k) },
        lambda = lambda,
        tidCounts = tidCounts
      )
    }
  }

}
