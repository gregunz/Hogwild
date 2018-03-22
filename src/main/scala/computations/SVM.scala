package computations

import computations.SVM._

case class SVM() {
  var weights: Weights = Map.empty

  def updateWeight(gradient: Gradient): Unit = {
    val i = weights.getOrElse(0, -1d) + 1
    weights = weights + (0 -> i)
  }
}

object SVM {
  type Weights = Map[Int, Double]
  type Gradient = Map[Int, Double]
  type Features = Map[Int, Double]
  type Label = Boolean

  def computeGradient(features: Features, label: Label, weights: Weights): Gradient = {
    features //TODO: should compute then return gradient
  }

}
