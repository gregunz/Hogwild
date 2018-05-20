package model

import utils.Label.Label
import utils.Types.{Counts, LearningRate}


class SVM(stepSize: LearningRate = 0.01) {
  var weights: SparseNumVector[Double] = SparseNumVector.empty

  /**
    * update the weights of the model and return the weights coordinates by how much they've changed
    *
    * @param gradient
    * @return weights update
    */
  def updateWeights(gradient: SparseNumVector[Double]): SparseNumVector[Double] = {
    val weightsUpdate = SparseNumVector(
      gradient.tids.map { k =>
        val weightUpdate = -stepSize * gradient.toMap(k)
        k -> weightUpdate
      }.toMap
    )
    addWeightsUpdate(weightsUpdate)
    weightsUpdate
  }

  def addWeightsUpdate(weightsUpdate: SparseNumVector[Double]): Unit = {
    weights += weightsUpdate
  }

  def loss(features: IndexedSeq[SparseNumVector[Double]], labels: IndexedSeq[Label], lambda: Double, tidCounts: Counts): Double = {
    require(features.size == labels.size)
    features.zip(labels)
      .map { case (f, l) =>
        val hinge = Math.max(0, 1 - (l.id * (f dot weights)))
        val w = weights.filter(f.tids)
        val reg = 0.5 * lambda * (w * w).mapTo{(k,v) => v / tidCounts(k)}.norm
        hinge + reg
      }.sum
  }
}

object SVM {
  def computeStochasticGradient(feature: SparseNumVector[Double],
                                label: Label,
                                weights: SparseNumVector[Double],
                                lambda: Double,
                                tidCounts: Counts): SparseNumVector[Double] = {
    val gradRightPart = SparseNumVector(
      feature.toMap.map { case (k, v) => k -> (lambda * weights.toMap.withDefaultValue(0d)(k) / tidCounts(k)) })
    if (label.id * (feature dot weights) >= 1) {
      gradRightPart
    } else {
      gradRightPart.pointWise(feature.mapTo((_, v) => v * (-label.id)), _ + _)
    }
  }
}
