package computations

import computations.SVM.{Feature, Weights}

object Operations {

  def pointWise(feature: Feature, weights: Weights, op: (Double, Double) => Double): Feature = {
    feature.map{ case(k, v) => k -> op(v, weights(k)) }
  }

  def dotProduct(feature: Feature, weights: Weights): Double = {
    pointWise(feature, weights, _ * _).values.sum
  }
}
