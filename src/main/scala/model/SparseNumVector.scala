package model

import utils.Types.TID

import scala.math.Numeric
import scala.math.Numeric.Implicits._

case class SparseNumVector[T: Numeric](private var vector: Map[TID, T] = Map.empty[TID, T]) {
  private[this] val zero = implicitly[Numeric[T]].zero
  vector = vector.withDefaultValue(zero)

  def toMap: Map[TID, T] = vector //.withDefaultValue(0d)
  def tids: Set[TID] = this.toMap.keySet

  def +(other: SparseNumVector[T]): SparseNumVector[T] = {
    this.pointWise(other, (a: T, b: T) => a + b)
  }

  def *(other: SparseNumVector[T]): SparseNumVector[T] = {
    this.pointWise(other, (a: T, b: T) => a * b)
  }

  def filter(tids: Set[TID]): SparseNumVector[T] = {
    SparseNumVector(this.toMap.filter{ case(k, v) => tids(k)})
  }

  def mapTo(op: (TID, T) => T): SparseNumVector[T] = {
    SparseNumVector(this.toMap.map { case (k, v) => k -> op(k, v) })
  }

  def dot(that: SparseNumVector[T]): T = {
    (this * that).norm
  }

  def pointWise(that: SparseNumVector[T], op: (T, T) => T): SparseNumVector[T] = {
    val keys = this.tids union that.tids
    SparseNumVector(keys.map(k => k -> op(this.toMap(k), that.toMap(k))).toMap)
  }

  def norm: T = this.toMap.values.foldLeft(zero)((a: T, b: T) => a + b)
}

object SparseNumVector {
  def empty[T: Numeric]: SparseNumVector[T] = SparseNumVector()
}