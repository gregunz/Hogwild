package model

import utils.Label.Label
import utils.Types.{Counts, LearningRate}


case class SVM(stepSize: LearningRate = 0.01) {
  var weights: SparseNumVector = SparseNumVector.empty

  /**
    * update the weights of the model and return the weights coordinates by how much they've changed
    *
    * @param gradient
    * @return weights update
    */
  def updateWeight(gradient: SparseNumVector): SparseNumVector = {
    SparseNumVector(
      gradient.values.keySet.map { k =>
        val weightUpdate = - stepSize * gradient.values(k)
        val newWeight = weights.values(k) + weightUpdate
        weights = SparseNumVector(weights.values + (k -> newWeight))
        k -> weightUpdate
      }.toMap
    )
  }

  def loss(features: IndexedSeq[SparseNumVector], labels: IndexedSeq[Label], lambda: Double, tidCounts: Counts): Double = {
    require(features.size == labels.size)
    features.zip(labels)
      .map { case (f, l) =>
        val hinge = Math.max(0, 1 - (l.id * f.dotProduct(weights)))
        val reg = 0.5 * lambda * f.mapTo { (k, v) => Math.pow(weights.values.withDefaultValue(0d)(k), 2) / tidCounts(k) }.values.values.sum
        hinge + reg
      }.sum
  }

}

object SVM {
  def computeStochasticGradient(feature: SparseNumVector,
                                label: Label,
                                weights: SparseNumVector,
                                lambda: Double,
                                tidCounts: Counts): SparseNumVector = {
    val gradRightPart = SparseNumVector(
      feature.values.map { case (k, v) => k -> (lambda * weights.values.withDefaultValue(0d)(k) / tidCounts(k)) })
    if (label.id * feature.dotProduct(weights) >= 1) {
      gradRightPart
    } else {
      gradRightPart.pointWise(feature.mapTo((_, v) => v * (-label.id)), _ + _)
    }
  }

}
