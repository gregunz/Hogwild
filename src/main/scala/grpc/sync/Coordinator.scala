package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import model._
import utils.SyncCoordinatorMode

import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {


  def run(mode: SyncCoordinatorMode): Unit = {
    val dataset = Dataset(mode.dataPath, mode.samples).fullLoad()
    val svm = new SVM()
    val workersAggregator = WorkersAggregator

    val service = WorkerService(dataset, svm, workersAggregator)
    val ssd = WorkerServiceSyncGrpc.bindService(service, ExecutionContext.global)
    println(">> READY <<")
    runServer(ssd, mode.port).awaitTermination()
  }


  case class WorkerService(dataset: Dataset, svm: SVM, workersAggregator: WorkersAggregator.type) extends WorkerServiceSyncGrpc.WorkerServiceSync {

    val samples: Iterator[Int] = dataset.samples().toIterator

    val subsetSize = 500
    val (someFeatures, someLabels) = dataset.getSubset(subsetSize).unzip
    private val instance = this
    var i = 0
    var time: Long = System.currentTimeMillis()

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
                    dataset.tidCounts
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
      instance.synchronized {
        workersAggregator.removeWorker()
        if (!workersAggregator.isWaitingOnSomeWorker) {
          svm.updateWeights(workersAggregator.getMeanGradient)
          instance.notifyAll()
        }
      }
    }

    private def spawnWorkerResponse(weights: SparseNumVector[Double]): WorkerResponse = {
      val did = samples.next
      WorkerResponse(
        did = did,
        weights = weights.filter(dataset.getFeature(did).tids).toMap
      )
    }
  }

}