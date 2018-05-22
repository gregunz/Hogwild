package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import model._
import utils.{SyncCoordinatorMode}

import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val svm = new SVM()
  val workersAggregator = new WorkersAggregator

  /* TO COMPUTE & PRINT LOSSES */
  val subsetSize = 500
  val (someFeatures, someLabels) = Dataset.getSubset(subsetSize).unzip
  var i = 0
  var time: Long = System.currentTimeMillis()

  def run(mode: SyncCoordinatorMode): Unit = {
      load()
      val ssd = WorkerServiceSyncGrpc.bindService(WorkerService, ExecutionContext.global)
      println(">> READY <<")
      runServer(ssd, mode.port)
  }

  def load(): Unit = {
    Dataset.fullLoad()
    samples
  }

  object WorkerService extends WorkerServiceSyncGrpc.WorkerServiceSync {

    private val instance = this

    override def updateWeights(responseObserver: StreamObserver[WorkerResponse]): StreamObserver[WorkerRequest] =
      new StreamObserver[WorkerRequest] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          safeRemoveWorker()
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          safeRemoveWorker()
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

    private def safeRemoveWorker(): Unit = {
      instance.synchronized{
        workersAggregator.removeWorker()
        if(!workersAggregator.isWaitingOnSomeWorker){
          svm.updateWeights(workersAggregator.getMeanGradient)
          instance.notifyAll()
        }
      }
    }

    private def spawnWorkerResponse(weights: SparseNumVector[Double]): WorkerResponse = {
      val did = samples.next
      WorkerResponse(
        did = did,
        weights = weights.filter(Dataset.getFeature(did).tids).toMap
      )
    }
  }

}