package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer, WeightsUpdateHandler}
import io.grpc.stub.StreamObserver
import model._
import utils.SyncCoordinatorMode

import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {


  def run(mode: SyncCoordinatorMode): Unit = {
    val dataset = Dataset(mode.dataPath, mode.samples).fullLoad()
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val workersAggregator = WorkersAggregator
    val weightsUpdateHandler = WeightsUpdateHandler(mode.interval, dataset)

    val service = WorkerService(dataset, svm, workersAggregator, weightsUpdateHandler)
    val ssd = WorkerServiceSyncGrpc.bindService(service, ExecutionContext.global)
    println(">> READY <<")
    runServer(ssd, mode.port).awaitTermination()
  }


  case class WorkerService(
                            dataset: Dataset,
                            svm: SVM,
                            workersAggregator: WorkersAggregator.type,
                            weightsUpdateHandler: WeightsUpdateHandler
                          ) extends WorkerServiceSyncGrpc.WorkerServiceSync {

    private val instance = this

    val samples: Iterator[Int] = dataset.samples().toIterator

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
                val weightsUpdate = svm.updateWeights(workersAggregator.getMeanGradient)
                weightsUpdateHandler.addWeightsUpdate(weightsUpdate)
                instance.notifyAll()
              }
            }
          } else {
            println("[NEW]: a worker wants to compute some gradients")
            workersAggregator.addWorker()
          }
          responseObserver.onNext(spawnWorkerResponse(weightsUpdateHandler.getAndResetWeightsUpdate()))
          if (weightsUpdateHandler.reachedInterval){
            weightsUpdateHandler.resetInterval()
            weightsUpdateHandler.showLoss(svm)
          }
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