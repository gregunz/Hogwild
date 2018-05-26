package grpc.async

import model.SparseNumVector

object WeightsUpdateHandler {
  private var weightsUpdateAggregated: SparseNumVector[Double] = SparseNumVector.empty

  def addWeightsUpdate(weightsUpdate: SparseNumVector[Double]): Unit = {
    weightsUpdateAggregated += weightsUpdate
  }

  def getAndResetWeightsUpdate(): SparseNumVector[Double] = {
    val tmp = weightsUpdateAggregated
    resetWeightsUpdate()
    tmp
  }

  def resetWeightsUpdate(): Unit = weightsUpdateAggregated = SparseNumVector.empty

}