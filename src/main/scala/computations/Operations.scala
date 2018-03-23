package computations

import computations.SVM.{Feature, Weights}

object Operations {

  def pointWise(features: Feature, weights: Weights, op: (Double, Double) => Double): Feature = {
    features.map{ case(k, v) => k -> op(v, weights(k)) }
  }

  def dotProduct(features: Feature, weights: Weights): Double = {
    pointWise(features, weights, _ * _).values.sum
  }
}
