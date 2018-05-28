package model

import utils.Label
import utils.Label.Label
import utils.Types.{LearningRate, TID}

import scala.collection.immutable.Queue


class SVM(lambda: Double, stepSize: LearningRate) {
  var weights: SparseNumVector[Double] = SparseNumVector.empty
  var losses: Queue[Double] = Queue.empty

  /**
    * update the weights of the model and return the weights coordinates by how much they've changed
    *
    * @param gradient
    * @return weights update
    */
  def updateWeights(gradient: SparseNumVector[Double]): SparseNumVector[Double] = {
    val weightsUpdate = SparseNumVector(
      gradient.keys.map { k =>
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

  def predictLabels(features: IndexedSeq[SparseNumVector[Double]]): IndexedSeq[Label] = {
    predict(features).map(pred => Label.fromInt(Math.round(pred).toInt))
  }

  def predict(features: IndexedSeq[SparseNumVector[Double]]): IndexedSeq[Double] = {
    features.map(_ dot weights)
  }

  def computeLoss(pred: Double, weightKeys: Set[TID], label: Label, inverseTidCountsVector: SparseNumVector[Double]): Double = {
    val hinge = Math.max(0, 1 - (label.id * pred))
    val w = weights.filterKeys(weightKeys)
    val reg = 0.5 * lambda * (w * w * inverseTidCountsVector).firstNorm
    val loss = hinge + reg
    loss
  }

  def lossAndAccuracy(testSet: Seq[(SparseNumVector[Double], Label)],
                      inverseTidCountsVector: SparseNumVector[Double]): (Double, Double) = {

    val (losses, correctPredictions) = testSet
      .par
      .map { case (feature, label) =>
        val pred = feature dot weights
        val loss = computeLoss(pred, feature.keys, label, inverseTidCountsVector)
        val correctPred = if (pred >= 0 && label.id == 1 || pred < 0 && label.id == -1) 1 else 0
        loss -> correctPred
      }.unzip

    val accuracy = correctPredictions.sum / correctPredictions.length.toDouble
    val loss = losses.sum / losses.length.toDouble

    loss -> accuracy
  }

  def computeStochasticGradient(feature: SparseNumVector[Double],
                                label: Label,
                                inverseTidCountsVector: SparseNumVector[Double]): SparseNumVector[Double] = {
    SVM.computeStochasticGradient(feature, label, weights, lambda, inverseTidCountsVector)
  }
}

object SVM {
  def computeStochasticGradient(feature: SparseNumVector[Double],
                                label: Label,
                                weights: SparseNumVector[Double],
                                lambda: Double,
                                inverseTidCountsVector: SparseNumVector[Double]): SparseNumVector[Double] = {

    val gradRightPart = weights.filterKeys(feature.keys) * lambda * inverseTidCountsVector
    if (label.id * (feature dot weights) >= 1) {
      gradRightPart
    } else {
      gradRightPart + (feature * (-label.id))
    }
  }
}
