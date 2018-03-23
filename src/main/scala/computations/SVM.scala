package computations

import computations.Label.Label
import computations.Operations.{dotProduct, pointWise}
import computations.SVM._


case class SVM(stepSize: LearningRate = 0.01) {
  var weights: Weights = Map.empty.withDefaultValue(0d)

  def updateWeight(gradient: Gradient): Unit = {
    gradient.keySet.foreach { k =>
      val w_k = weights(k) - stepSize * gradient(k)
      weights = weights + (k -> w_k)
    }
  }
}

object SVM {
  type LearningRate = Double
  type Weights = Map[Int, Double]
  type Gradient = Map[Int, Double]
  type Feature = Map[Int, Double]
  type Counts = Map[Int, Int]


  def computeGradient(features: Feature,
                      label: Label,
                      weights: Weights,
                      lambda: Double,
                      tidCounts: Counts): Gradient = {
    val gradRightPart = features.map { case (k, v) => k -> (lambda * weights.withDefaultValue(0d)(k) / tidCounts(k)) }
    if (label.id * dotProduct(features, weights) >= 1) {
      gradRightPart
    } else {
      pointWise(features.mapValues(_ * (-label.id)), gradRightPart, _ + _)
    }
  }

}
