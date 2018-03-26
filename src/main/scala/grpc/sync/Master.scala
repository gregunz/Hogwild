package grpc.sync

import java.util.concurrent.TimeUnit

import computations.SVM
import database.DB
import dataset.Dataset
import io.grpc.stub.StreamObserver
import utils.Label
import utils.Label.Label
import utils.Types.{Counts, SparseVector}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

object Master extends GrpcServer {

  lazy val samples: Iterator[(SparseVector, Label, Counts)] = Dataset.samples().toIterator
  val lambda: Double = 0.1

  val svm = SVM()
  private val instance = this

  private var i = 0
  private var time = System.currentTimeMillis()
  private val someDids = Dataset.dids.toIndexedSeq.take(1000)
  private val someFeatures = someDids.map(Dataset.getFeature)
  private val someLabels = someDids.map(Dataset.didToLabel)

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
    println(Dataset.tidCounts.values.sum)
  }

  object SlaveService extends SlaveServiceGrpc.SlaveService {

    override def updateWeights(responseObserver: StreamObserver[SlaveResponse]): StreamObserver[SlaveRequest] =
      new StreamObserver[SlaveRequest] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
        }

        def onNext(req: SlaveRequest): Unit = {
          instance.synchronized {
            if (req.gradient.nonEmpty) {
              if (i % 10000 == 0) {
                val loss = svm.loss(
                  someFeatures,
                  someLabels,
                  lambda,
                  Dataset.tidCounts
                )
                val duration = System.currentTimeMillis() - time
                time = System.currentTimeMillis()
                println(s"[UPT][$i][$duration]: loss = $loss}")
              }
              i += 1

              svm.updateWeight(req.gradient)
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
        tidCounts = feature.map { case (k, _) => k -> tidCounts.withDefaultValue(0)(k) }
      )
    }
  }

}
