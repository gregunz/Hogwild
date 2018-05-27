package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import launcher.mode.SyncCoordinatorMode
import model._
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {


  def run(mode: SyncCoordinatorMode): Unit = {
    val dataset = mode.dataset.getReady(mode.isMaster)
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)

    val service = WorkerService(mode.logger, dataset, svm, mode.stoppingCriteria)
    val ssd = WorkerServiceSyncGrpc.bindService(service, ExecutionContext.global)
    mode.logger.log(2)("Coordinator ready ")
    runServer(ssd, mode.port).awaitTermination()
  }


  case class WorkerService(
                            logger: Logger,
                            dataset: Dataset,
                            svm: SVM,
                            stoppingCriteria: StoppingCriteria,
                          ) extends WorkerServiceSyncGrpc.WorkerServiceSync {

    private val instance = this
    private var weightsUpdate = SparseNumVector.empty[Double]
    private var lossComputingFuture = Future.successful()

    override def updateWeights(responseObserver: StreamObserver[WorkerResponse]): StreamObserver[WorkerRequest] = {
      new StreamObserver[WorkerRequest] {
        def onError(t: Throwable): Unit = {
          logger.log(2)(s"One worker left. (on error)")
          safeRemoveWorker()
          if (stoppingCriteria.shouldStop && !WorkersAggregator.noWorkersAvailable) {
            stoppingCriteria.export()
            sys.exit(0)
          }
        }

        def onCompleted(): Unit = {
          logger.log(2)(s"One worker left. (on completed)")
          safeRemoveWorker()
        }

        def onNext(req: WorkerRequest): Unit = {
          instance.synchronized {
            if (stoppingCriteria.shouldStop) {
              responseObserver.onNext(WorkerResponse(weightsUpdate = Map.empty))
            } else {
              if (req.gradient.nonEmpty) {
                WorkersAggregator.addGradient(SparseNumVector(req.gradient))
                if (WorkersAggregator.isWaitingOnSomeWorker) {
                  instance.wait()
                } else {
                  weightsUpdate = svm.updateWeights(WorkersAggregator.getMeanGradient)
                  if (stoppingCriteria.interval.hasReachedOrKeepGoing && lossComputingFuture.isCompleted) {
                    lossComputingFuture = Future {
                      stoppingCriteria.compute(svm, displayLoss = true)
                    }
                  }
                  instance.notifyAll()
                }
                responseObserver.onNext(WorkerResponse(
                  weightsUpdate = weightsUpdate.toMap
                ))
              } else {
                logger.log(2)("[NEW]: a worker wants to compute some gradients")
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