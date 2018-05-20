package grpc.async

import model.SparseNumVector

class WeightsUpdateHandler {
  private var weightsUpdateAggregated: SparseNumVector[Double] = SparseNumVector.empty
  private var counter = 0

  def counts: Int = counter

  def addWeightsUpdate(weightsUpdate: SparseNumVector[Double]): Unit = {
    counter += 1
    weightsUpdateAggregated += weightsUpdate
  }

  def getAndResetWeighsUpdate(): SparseNumVector[Double] = {
    counter = 0
    val tmp = weightsUpdateAggregated
    weightsUpdateAggregated = SparseNumVector.empty
    tmp
  }

}