package model

import utils.Types.TID

import scala.math.Numeric
import scala.math.Numeric.Implicits._

case class SparseNumVector[T: Numeric](private var vector: Map[TID, T] = Map.empty[TID, T]) {
  private[this] val zero = implicitly[Numeric[T]].zero
  vector = vector.withDefaultValue(zero) // wish to set it as val and force this !

  def filterKeys(predicate: TID => Boolean): SparseNumVector[T] = {
    SparseNumVector(this.toMap.filterKeys(predicate))
  }

  def +(other: SparseNumVector[T]): SparseNumVector[T] = {
    this.pointWise(other, _ + _)
  }

  def +(value: T): SparseNumVector[T] = {
    this.mapValues(_ + value)
  }

  def -(other: SparseNumVector[T]): SparseNumVector[T] = {
    this.pointWise(other, _ - _)
  }

  def -(value: T): SparseNumVector[T] = {
    this.mapValues(_ - value)
  }

  def mapValues(op: T => T): SparseNumVector[T] = {
    this.map((_, v) => op(v))
  }

  def map(op: (TID, T) => T): SparseNumVector[T] = {
    SparseNumVector(this.toMap.map { case (k, v) => k -> op(k, v) })
  }

  def toMap: Map[TID, T] = vector

  def *(value: T): SparseNumVector[T] = {
    this.mapValues(_ * value)
  }

  def dot(that: SparseNumVector[T]): T = {
    (this * that).firstNorm
  }

  def *(other: SparseNumVector[T]): SparseNumVector[T] = {
    this.pointWise(other, _ * _, (a, b) => a.intersect(b))
  }

  def pointWise(
                 that: SparseNumVector[T],
                 op: (T, T) => T,
                 keysOp: (Set[TID], Set[TID]) => Set[TID] = (a, b) => a.union(b)
               ): SparseNumVector[T] = {
    val keys = keysOp(this.tids, that.tids)
    SparseNumVector(keys.map(k => k -> op(this.toMap(k), that.toMap(k))).toMap)
  }

  def tids: Set[TID] = this.toMap.keySet

  def firstNorm: T = this.toMap.values.foldLeft(zero)(_ + _)
}

object SparseNumVector {
  def empty[T: Numeric]: SparseNumVector[T] = SparseNumVector()
}