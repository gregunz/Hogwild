package computations

import computations.SVM.SparseVector

object Operations {

  def pointWise(vect1: SparseVector, vect2: SparseVector, op: (Double, Double) => Double): SparseVector = {
    vect1.map { case (k, v) => k -> op(v, vect2(k)) }
  }

  def dotProduct(vect1: SparseVector, vect2: SparseVector): Double = {
    pointWise(vect1, vect2, _ * _).values.sum
  }
}
