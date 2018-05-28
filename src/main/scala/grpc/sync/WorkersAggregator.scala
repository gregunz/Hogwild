package grpc.sync

import model.SparseNumVector


object WorkersAggregator {
  private var numWorkers = 0
  private var gradients: List[SparseNumVector[Double]] = List()


  def num: Int = numWorkers

  def addWorker(): Unit = {
    numWorkers += 1
  }

  def removeWorker(): Unit = {
    numWorkers -= 1
  }

  def isWaitingOnSomeWorker: Boolean = {
    gradients.size < numWorkers
  }

  def addGradient(gradient: SparseNumVector[Double]): Unit = {
    gradients ::= gradient
  }

  def getMeanGradient: SparseNumVector[Double] = {
    val meanGradient = gradients
      .foldLeft(SparseNumVector.empty[Double])(_ + _)
      .mapValues(_ / Math.max(1, gradients.size))
    gradients = List()
    meanGradient
  }

  def noWorkersAvailable: Boolean = numWorkers == 0

}