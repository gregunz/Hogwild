package grpc.sync

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import launcher.mode.SyncCoordinatorMode
import model._
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Coordinator extends GrpcServer with GrpcRunnable[SyncCoordinatorMode] {

  var exportOnce: Boolean = true

  def run(mode: SyncCoordinatorMode): Unit = {

    Random.setSeed(mode.seed)

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
          instance.synchronized{
            if(stoppingCriteria.shouldStop && exportOnce){
              exportOnce = false
              stoppingCriteria.export()
              logger.alwaysLog("I am done!")
              sys.exit(0)
            }
          }
        }

        def onCompleted(): Unit = {
          logger.log(2)(s"One worker left. (on completed)")
          safeRemoveWorker()
        }

        def onNext(req: WorkerRequest): Unit = {
          if (stoppingCriteria.shouldStop) {
            responseObserver.onNext(WorkerResponse(weightsUpdate = Map.empty, stop = true))
          } else {
            instance.synchronized {
              if (req.gradient.nonEmpty) {
                WorkersAggregator.addGradient(SparseNumVector(req.gradient))
                if (WorkersAggregator.isWaitingOnSomeWorker) {
                  //logger.log(3)("[RECEIVED] thanks for your gradient(s) worker! still waiting for some workers...")
                  instance.wait()
                } else {
                  //logger.log(3)(s"[RECEIVED] thanks you all (${WorkersAggregator.num} worker(s)) for the gradient(s)!")
                  if (stoppingCriteria.interval.hasReachedOrFirst && lossComputingFuture.isCompleted) {
                    stoppingCriteria.interval.reset()
                    lossComputingFuture = Future {
                      stoppingCriteria.computeStats(svm, displayStats = true)
                    }
                  }
                  weightsUpdate = svm.updateWeights(WorkersAggregator.getMeanGradient)
                  instance.notifyAll()
                }
                responseObserver.onNext(WorkerResponse(
                  weightsUpdate = weightsUpdate.toMap,
                  stop = false
                ))
              } else {
                logger.log(2)("[NEW] a worker wants to compute some gradients")
                responseObserver.onNext(WorkerResponse(weightsUpdate = svm.weights.toMap, stop = false))
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