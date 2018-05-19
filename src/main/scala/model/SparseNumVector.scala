package model

import utils.Types.TID

case class SparseNumVector(var values: Map[TID, Double]) {
  values = values.withDefaultValue(0d)

  def +(other: SparseNumVector): SparseNumVector = {
    this.pointWise(other, _ + _)
  }

  def mapTo[T](op: (TID, Double) => Double): SparseNumVector = {
    SparseNumVector(this.values.map { case (k, v) => k -> op(k, v) })
  }

  def dotProduct(that: SparseNumVector): Double = {
    this.pointWise(that, _ * _).values.values.sum
  }

  def pointWise(that: SparseNumVector, op: (Double, Double) => Double): SparseNumVector = {
    val keys = this.values.keySet union that.values.keySet
    SparseNumVector(keys.map(k => k -> op(this.values(k), that.values(k))).toMap)
  }

}

object SparseNumVector {
  def empty: SparseNumVector = SparseNumVector(Map.empty.withDefaultValue(0d))
}