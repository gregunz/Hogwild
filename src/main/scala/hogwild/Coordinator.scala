package hogwild

import dataset.Dataset
import io.grpc.stub.StreamObserver
//import model.{SVM, SlavesHandler, SparseNumVector}
import utils.Label.Label
import utils.Types.TID
import model._

import model.GrpcServer
import hogwild.grpc.WorkerServiceGrpc.WorkerServiceStub
import hogwild.grpc._

import scala.collection.immutable
import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val lambda: Double = 0.1
  val svm = SVM()

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
    runServer(ssd, 50050)
  }

  def load(): Unit = {
    Dataset.load()
    samples
  }

  object SlaveService extends SlaveServiceGrpc.SlaveService {

    val instance = this

    override def updateWeights(responseObserver: StreamObserver[SlaveResponse]): StreamObserver[SlaveRequest] =
      new StreamObserver[SlaveRequest] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          slavesHandler.removeSlave()
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          slavesHandler.removeSlave()
        }

        def onNext(req: SlaveRequest): Unit = {
          if (req.gradient.nonEmpty) {
            instance.synchronized {
              slavesHandler.addGradient(SparseNumVector(req.gradient))
              if(slavesHandler.isWaitingOnSomeSlave){
                instance.wait()
              } else {
                svm.updateWeight(slavesHandler.getMeanGradient)
                if (i % 500 == 0) {
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
                instance.notifyAll()
              }
            }
          } else {
            println("[NEW]: a slave wants to compute some gradients")
            slavesHandler.addSlave()
          }
          responseObserver.onNext(spawnSlaveResponse(svm.weights))
        }
      }

    private def spawnSlaveResponse(weights: SparseNumVector): SlaveResponse = {
      val did = samples.next
      SlaveResponse(
        did = did,
        weights = Dataset.getFeature(did).mapTo { case (k, _) => weights.values.withDefaultValue(0d)(k) }.values
      )
    }
  }
}

