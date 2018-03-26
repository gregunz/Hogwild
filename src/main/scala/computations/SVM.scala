package computations

import computations.Operations.{dotProduct, pointWise}
import utils.Label.Label
import utils.Types.{Counts, LearningRate, SparseVector}


case class SVM(stepSize: LearningRate = 0.1) {
  var weights: SparseVector = Map.empty.withDefaultValue(0d)

  def updateWeight(gradient: SparseVector): Unit = {
    gradient.keySet.foreach { k =>
      val w_k = weights(k) - stepSize * gradient(k)
      weights = weights + (k -> w_k)
    }
  }

  def loss(features: IndexedSeq[SparseVector], labels: IndexedSeq[Label], lambda: Double, tidCounts: Counts): Double = {
    require(features.size == labels.size)
    features.zip(labels)
      .map { case (f, l) =>
        val hinge = Math.max(0, 1 - (l.id * Operations.dotProduct(f, weights)))
        val reg = 0.5 * lambda * f.map { case (k, _) => Math.pow(weights.withDefaultValue(0d)(k), 2) / tidCounts(k) }.sum
        hinge + reg
      }.sum
  }
}

object SVM {
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
