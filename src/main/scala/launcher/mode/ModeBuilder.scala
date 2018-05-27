package launcher.mode

import dataset.Dataset
import model.StoppingCriteria
import utils.Types.LearningRate
import utils.{Interval, Logger}

case class ModeBuilder(name: Option[String], logger: Logger, dataset: Dataset, lambda: Double, stepSize: LearningRate) {
  def build(serverIp: String, serverPort: Int) =
    SyncWorkerMode(name = name, logger = logger, dataset = dataset, lambda = lambda, stepSize = stepSize, serverIp = serverIp,
      serverPort = serverPort)

  def build(port: Int, stoppingCriteria: StoppingCriteria) =
    SyncCoordinatorMode(name = name, logger = logger, dataset = dataset, lambda = lambda, stepSize = stepSize, port = port,
      stoppingCriteria = stoppingCriteria)

  def build(port: Int, workerIp: String, workerPort: Int, stoppingCriteria: Option[StoppingCriteria],
            broadcastInterval: Interval) =
    AsyncWorkerMode(name = name, logger = logger, dataset = dataset, lambda = lambda, stepSize = stepSize,
      broadcastInterval = broadcastInterval, port = port, workerIp = workerIp, workerPort = workerPort,
      stoppingCriteria = stoppingCriteria)
}
