package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import model._
import utils.{Interval, SyncCoordinatorMode}

import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {


  def run(mode: SyncCoordinatorMode): Unit = {
    val dataset = Dataset(mode.dataPath, mode.samples).fullLoad()
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)

    val service = WorkerService(dataset, svm, mode.interval)
    val ssd = WorkerServiceSyncGrpc.bindService(service, ExecutionContext.global)
    println(">> READY <<")
    runServer(ssd, mode.port).awaitTermination()
  }


  case class WorkerService(
                            dataset: Dataset,
                            svm: SVM,
                            interval: Interval,
                          ) extends WorkerServiceSyncGrpc.WorkerServiceSync {

    private val instance = this
    private var weightsUpdate = SparseNumVector.empty[Double]

    val samples: Iterator[Int] = dataset.samples().toIterator
    val stoppingCriterion = StoppingCriterion(dataset)

    override def updateWeights(responseObserver: StreamObserver[WorkerResponse]): StreamObserver[WorkerRequest] = {
      new StreamObserver[WorkerRequest] {
        def onError(t: Throwable): Unit = {
          println(s"One worker left.")
          safeRemoveWorker()
          if (stoppingCriterion.shouldStop && !WorkersAggregator.noWorkersAvailable) {
            WeightsExport.uploadWeightsAndGetLink(svm.weights)
            sys.exit(0)
          }
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          safeRemoveWorker()
        }

        def onNext(req: WorkerRequest): Unit = {
          if (stoppingCriterion.shouldStop) {
            responseObserver.onNext(WorkerResponse(did = -1, weights = Map.empty))
          } else {
            if (req.gradient.nonEmpty) {
              instance.synchronized {
                WorkersAggregator.addGradient(SparseNumVector(req.gradient))
                if (WorkersAggregator.isWaitingOnSomeWorker) {
                  instance.wait()
                } else {
                  weightsUpdate = svm.updateWeights(WorkersAggregator.getMeanGradient)
                  instance.notifyAll()
                }
              }
            } else {
              println("[NEW]: a worker wants to compute some gradients")
              WorkersAggregator.addWorker()
            }
            responseObserver.onNext(spawnWorkerResponse(weightsUpdate))
          }
          if (interval.resetIfReachedElseIncrease()) {
            stoppingCriterion.compute(svm, displayLoss = true)
          }
        }
      }
    }


    private def spawnWorkerResponse(weights: SparseNumVector[Double]): WorkerResponse = {
      val did = samples.next
      WorkerResponse(
        did = did,
        weights = weights.filterKeys(dataset.getFeature(did).tids).toMap
      )
    }

    private def safeRemoveWorker(): Unit = {
      instance.synchronized {
        WorkersAggregator.removeWorker()
        if (!WorkersAggregator.isWaitingOnSomeWorker) {
          svm.updateWeights(WorkersAggregator.getMeanGradient)
          instance.notifyAll()
        }
      }
    }
  }

}