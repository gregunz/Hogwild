package computations

import computations.Label.Label
import computations.Operations.{dotProduct, pointWise}
import computations.SVM._


case class SVM(stepSize: LearningRate = 0.1) {
  var weights: SparseVector = Map.empty.withDefaultValue(0d)

  def updateWeight(gradient: SparseVector): Unit = {
    gradient.keySet.foreach { k =>
      val w_k = weights(k) - stepSize * gradient(k)
      weights = weights + (k -> w_k)
    }
  }
}

object SVM {
  type LearningRate = Double
  type TID = Int
  type SparseVector = Map[TID, Double]
  type Counts = Map[TID, Int]


  def computeStochasticGradient(feature: SparseVector,
                                label: Label,
                                weights: SparseVector,
                                lambda: Double,
                                tidCounts: Counts): SparseVector = {
    val gradRightPart = feature.map { case (k, v) => k -> (lambda * weights.withDefaultValue(0d)(k) / tidCounts(k)) }
    if (label.id * dotProduct(feature, weights) >= 1) {
      gradRightPart
    } else {
      pointWise(feature.mapValues(_ * (-label.id)), gradRightPart, _ + _)
    }
  }

}
