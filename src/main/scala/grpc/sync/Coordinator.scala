package grpc.sync

import dataset.Dataset
import io.grpc.stub.StreamObserver
import launcher.GrpcRunnable
import model._

import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer with GrpcRunnable {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val lambda: Double = 0.1
  val svm = new SVM()

  val workersAggregator = new WorkersAggregator

  /* TO COMPUTE & PRINT LOSSES */
  val subsetSize = 500
  val (someFeatures, someLabels) = Dataset.getSubset(subsetSize).unzip
  var i = 0
  var time: Long = System.currentTimeMillis()

  def run(args: Seq[String]): Unit = {
    args match {
      case port :: _ =>
        load()
        val ssd = WorkerServiceSyncGrpc.bindService(WorkerService, ExecutionContext.global)
        println(">> READY <<")
        runServer(ssd, port.toInt)

      case _ => argMismatch(s"expecting port but get $args")
    }
  }

  def load(): Unit = {
    Dataset.load()
    samples
  }

  object WorkerService extends WorkerServiceSyncGrpc.WorkerServiceSync {

    private val instance = this

    override def updateWeights(responseObserver: StreamObserver[WorkerResponse]): StreamObserver[WorkerRequest] =
      new StreamObserver[WorkerRequest] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          workersAggregator.removeWorker()
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          workersAggregator.removeWorker()
        }

        def onNext(req: WorkerRequest): Unit = {
          if (req.gradient.nonEmpty) {
            instance.synchronized {
              workersAggregator.addGradient(SparseNumVector(req.gradient))
              if (workersAggregator.isWaitingOnSomeWorker) {
                instance.wait()
              } else {
                svm.updateWeights(workersAggregator.getMeanGradient)
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
            println("[NEW]: a Worker wants to compute some gradients")
            workersAggregator.addWorker()
          }
          responseObserver.onNext(spawnWorkerResponse(svm.weights))
        }
      }

    private def spawnWorkerResponse(weights: SparseNumVector): WorkerResponse = {
      val did = samples.next
      WorkerResponse(
        did = did,
        weights = Dataset.getFeature(did).mapTo { case (k, _) => weights.values.withDefaultValue(0d)(k) }.values
      )
    }
  }

}