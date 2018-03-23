package computations

import util.Types.SparseVector

object Operations {

  def dotProduct(vect1: SparseVector, vect2: SparseVector): Double = {
    pointWise(vect1, vect2, _ * _).values.sum
  }

  def pointWise(vect1: SparseVector, vect2: SparseVector, op: (Double, Double) => Double): SparseVector = {
    vect1.map { case (k, v) => k -> op(v, vect2(k)) }
  }
}
