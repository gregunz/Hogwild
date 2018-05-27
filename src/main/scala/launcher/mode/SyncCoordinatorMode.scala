package launcher.mode

import dataset.Dataset
import grpc.sync.{Coordinator => SyncCoordinator}
import model.StoppingCriteria
import utils.Logger
import utils.Types.LearningRate

case class SyncCoordinatorMode(name: Option[String], logger: Logger, dataset: Dataset, lambda: Double, stepSize: LearningRate,
                               port: Int, stoppingCriteria: StoppingCriteria) extends TopMode {

  def isMaster = true

  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}
