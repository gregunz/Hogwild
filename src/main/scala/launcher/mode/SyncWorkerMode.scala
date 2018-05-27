package launcher.mode

import dataset.Dataset
import grpc.sync.{Worker => SyncWorker}
import utils.Logger
import utils.Types.LearningRate

case class SyncWorkerMode(name: Option[String], logger: Logger, dataset: Dataset, lambda: Double, stepSize: LearningRate,
                          serverIp: String, serverPort: Int)
  extends TopMode {
  def isMaster = false

  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}
