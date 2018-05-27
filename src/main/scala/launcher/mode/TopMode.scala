package launcher.mode

import dataset.Dataset
import utils.Logger
import utils.Types.LearningRate

trait TopMode extends Mode {
  val name: Option[String]
  val dataset: Dataset
  val lambda: Double
  val stepSize: LearningRate
  val logger: Logger

  def isMaster: Boolean

  def isSlave: Boolean = !isMaster
}
