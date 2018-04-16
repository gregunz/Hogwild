package model

import utils.Types.TID

case class SparseNumVector(values: Map[TID, Double]){

  def mapTo[T](op: (TID, Double) => Double): SparseNumVector = {
    SparseNumVector(this.values.map { case (k, v) => k -> op(k, v) })
  }

  def dotProduct(that: SparseNumVector): Double = {
    this.pointWise(that, _ * _).values.values.sum
  }

  def pointWise(that: SparseNumVector, op: (Double, Double) => Double): SparseNumVector = {
    val keys = this.values.keySet intersect that.values.keySet
    SparseNumVector(keys.map(k => k -> op(this.values(k), that.values(k))).toMap)
  }

}

object SparseNumVector {
  def empty: SparseNumVector = SparseNumVector(Map.empty)
}