package launcher.mode

import dataset.Dataset
import grpc.async.{Worker => AsyncWorker}
import model.StoppingCriteria
import utils.Types.LearningRate
import utils.{Interval, Logger}

case class AsyncWorkerMode(seed: Long, name: Option[String], logger: Logger, dataset: Dataset, lambda: Double, stepSize: LearningRate, port: Int,
                           workerIp: String, workerPort: Int, stoppingCriteria: Option[StoppingCriteria], broadcastInterval: Interval)
  extends TopMode {

  def isMaster: Boolean = stoppingCriteria.isDefined

  def run(): Unit = {
    logger.log(2)(printMode(this))
    AsyncWorker.run(this)
  }
}
