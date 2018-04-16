package grpc.sync

import computations.SVM
import dataset.Dataset
import io.grpc.stub.StreamObserver
import model.{SlavesHandler, SparseNumVector}
import utils.Label
import utils.Label.Label
import utils.Types.TID

import scala.collection.immutable
import scala.concurrent.ExecutionContext

object Master extends GrpcServer {

  lazy val samples: Iterator[(SparseNumVector, Label, Map[TID, Int])] = Dataset.samples().toIterator
  val lambda: Double = 0.1

  val svm = SVM()
  private val instance = this

  val slavesHandler = new SlavesHandler

  /* TO COMPUTE & PRINT LOSSES */
  val someDids: Set[TID] = Dataset.didSet.take(500)
  val someFeatures: immutable.IndexedSeq[SparseNumVector] = Dataset.features.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  val someLabels: immutable.IndexedSeq[Label] = Dataset.labels.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  var i = 0
  var time: Long = System.currentTimeMillis()

  def main(args: Array[String]): Unit = {
    load()
    val ssd = SlaveServiceGrpc.bindService(SlaveService, ExecutionContext.global)
    println(">> READY <<")
    runServer(ssd)
  }

  def load(): Unit = {
    Dataset.load()
    samples
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
          if (req.gradient.nonEmpty) {
            if (i % 1000 == 0) {
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

            instance.synchronized{
              svm.updateWeight(SparseNumVector(req.gradient))
            }
          } else {
            slavesHandler.addSlave(req.id)
            println("[NEW]: a slave wants to compute some gradients")
          }
          responseObserver.onNext(spawnSlaveResponse(svm.weights))
        }
      }

    private def spawnSlaveResponse(weights: SparseNumVector): SlaveResponse = {
      val (feature, label, tidCounts) = samples.next
      SlaveResponse(
        feature = feature.values,
        label = label == Label.CCAT,
        weights = feature.mapTo { case (k, _) => weights.values.withDefaultValue(0d)(k) }.values,
        lambda = lambda,
        tidCounts = tidCounts
      )
    }
  }

}