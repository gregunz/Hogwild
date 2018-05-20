package model

import utils.Types.TID

import scala.math.Numeric
import scala.math.Numeric.Implicits._

case class SparseNumVector[T: Numeric](private var vector: Map[TID, T] = Map.empty[TID, T]) {
  private[this] val zero = implicitly[Numeric[T]].zero
  vector = vector.withDefaultValue(zero) // wish to set it as val and force this !

  def toMap: Map[TID, T] = vector.filter{case(k,v) => v != zero} // not sure if need
  def tids: Set[TID] = this.toMap.keySet

  def pointWise(
                 that: SparseNumVector[T],
                 op: (T, T) => T,
                 keysOp: (Set[TID], Set[TID]) => Set[TID] = (a, b) => a.union(b)
               ): SparseNumVector[T] = {
    val keys = keysOp(this.tids, that.tids)
    SparseNumVector(keys.map(k => k -> op(this.toMap(k), that.toMap(k))).toMap)
  }

  def filter(tids: Set[TID]): SparseNumVector[T] = {
    SparseNumVector(this.toMap.filter{ case(k, v) => tids(k)})
  }

  def map(op: (TID, T) => T): SparseNumVector[T] = {
    SparseNumVector(this.toMap.map { case (k, v) => k -> op(k, v) })
  }

  def mapValues(op: T => T): SparseNumVector[T] = {
    this.map((_, v) => op(v))
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

  def *(other: SparseNumVector[T]): SparseNumVector[T] = {
    this.pointWise(other, _ * _, (a, b) => a.intersect(b))
  }

  def *(value: T): SparseNumVector[T] = {
    this.mapValues(_ * value)
  }

  def dot(that: SparseNumVector[T]): T = {
    (this * that).firstNorm
  }

  def firstNorm: T = this.toMap.values.foldLeft(zero)(_ + _)
}

object SparseNumVector {
  def empty[T: Numeric]: SparseNumVector[T] = SparseNumVector()
}