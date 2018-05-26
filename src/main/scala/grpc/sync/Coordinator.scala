package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import launcher.SyncCoordinatorMode
import model._
import utils.Interval

import scala.concurrent.ExecutionContext

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {


  def run(mode: SyncCoordinatorMode): Unit = {
    val dataset = Dataset(mode.dataPath).getReady(mode.isMaster)
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val stoppingCriterion = StoppingCriterion(dataset, mode.maxTimesWithoutImproving)

    val service = WorkerService(dataset, svm, mode.interval, stoppingCriterion)
    val ssd = WorkerServiceSyncGrpc.bindService(service, ExecutionContext.global)
    println(">> READY <<")
    runServer(ssd, mode.port).awaitTermination()
  }


  case class WorkerService(
                            dataset: Dataset,
                            svm: SVM,
                            interval: Interval,
                            stoppingCriterion: StoppingCriterion,
                          ) extends WorkerServiceSyncGrpc.WorkerServiceSync {

    private val instance = this
    private var weightsUpdate = SparseNumVector.empty[Double]

    override def updateWeights(responseObserver: StreamObserver[WorkerResponse]): StreamObserver[WorkerRequest] = {
      new StreamObserver[WorkerRequest] {
        def onError(t: Throwable): Unit = {
          println(s"One worker left.")
          safeRemoveWorker()
          if (stoppingCriterion.shouldStop && !WorkersAggregator.noWorkersAvailable) {
            WeightsExport.uploadWeightsAndGetLink(stoppingCriterion.getWeights)
            sys.exit(0)
          }
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          safeRemoveWorker()
        }

        def onNext(req: WorkerRequest): Unit = {
          instance.synchronized {
            if (stoppingCriterion.shouldStop) {
              responseObserver.onNext(WorkerResponse(weightsUpdate = Map.empty))
            } else {
              if (req.gradient.nonEmpty) {
                WorkersAggregator.addGradient(SparseNumVector(req.gradient))
                if (WorkersAggregator.isWaitingOnSomeWorker) {
                  instance.wait()
                } else {
                  weightsUpdate = svm.updateWeights(WorkersAggregator.getMeanGradient)
                  if (interval.resetIfReachedElseIncrease()) {
                    stoppingCriterion.compute(svm, displayLoss = true)
                  }
                  instance.notifyAll()
                }
                responseObserver.onNext(WorkerResponse(
                  weightsUpdate = weightsUpdate.toMap
                ))
              } else {
                println("[NEW]: a worker wants to compute some gradients")
                WorkersAggregator.addWorker()
              }
            }
          }
        }
      }
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