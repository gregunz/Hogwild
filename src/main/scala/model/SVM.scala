package model

import utils.Label.Label
import utils.Types.{Counts, LearningRate}


class SVM(lambda: Double, stepSize: LearningRate) {
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

  def loss(features: IndexedSeq[SparseNumVector[Double]], labels: IndexedSeq[Label], tidCounts: Counts): Double = {
    require(features.size == labels.size)
    val inverseTidCountsVector = SparseNumVector(tidCounts.mapValues(1d / _))

    features.zip(labels)
      .map { case (f, l) =>
        val hinge = Math.max(0, 1 - (l.id * (f dot weights)))
        val w = weights.filter(f.tids)
        val reg = 0.5 * lambda * (w * w * inverseTidCountsVector).firstNorm
        hinge + reg
      }.sum
  }

  def computeStochasticGradient(feature: SparseNumVector[Double],
                                label: Label,
                                tidCounts: Counts): SparseNumVector[Double] = {
    SVM.computeStochasticGradient(feature, label, weights, lambda, tidCounts)
  }
}

object SVM {
  def computeStochasticGradient(feature: SparseNumVector[Double],
                                label: Label,
                                weights: SparseNumVector[Double],
                                lambda: Double,
                                tidCounts: Counts): SparseNumVector[Double] = {

    val inverseTidCountsVector = SparseNumVector(tidCounts.mapValues(1d / _))
    val gradRightPart = weights.filter(feature.tids) * lambda * inverseTidCountsVector
    if (label.id * (feature dot weights) >= 1) {
      gradRightPart
    } else {
      gradRightPart + (feature * (-label.id))
    }
  }
}
