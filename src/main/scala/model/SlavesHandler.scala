package model

import scala.collection.mutable


class SlavesHandler {
  private var numSlaves = 0
  private var gradients: List[SparseNumVector] = List()

  def addSlave(): Unit = {
    numSlaves += 1
  }

  def removeSlave(): Unit = {
    numSlaves -= 1
  }

  def isWaitingOnSomeSlave: Boolean = {
    gradients.size < numSlaves
  }

  def addGradient(gradient: SparseNumVector): Unit = {
    gradients ::= gradient
  }

  def getMeanGradient: SparseNumVector = {
    val meanGradient = gradients.reduce(_ + _).mapTo((k,v) => v / gradients.size)
    gradients = List()
    meanGradient
  }

}
