package grpc.sync

import model.SparseNumVector


class WorkersAggregator {
  private var numWorkers = 0
  private var gradients: List[SparseNumVector] = List()

  def addWorker(): Unit = {
    numWorkers += 1
  }

  def removeWorker(): Unit = {
    numWorkers += 1
  }

  def isWaitingOnSomeWorker: Boolean = {
    gradients.size < numWorkers
  }

  def addGradient(gradient: SparseNumVector): Unit = {
    gradients ::= gradient
  }

  def getMeanGradient: SparseNumVector = {
    val meanGradient = gradients.reduce(_ + _).mapTo((k, v) => v / gradients.size)
    gradients = List()
    meanGradient
  }

}