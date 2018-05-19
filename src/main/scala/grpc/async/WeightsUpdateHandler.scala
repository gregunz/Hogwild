package grpc.async

import model.SparseNumVector

class WeightsUpdateHandler {
  private var weightsUpdateAggregated: SparseNumVector = SparseNumVector.empty
  private var counts = 0

  def getCounts(): Int = counts

  def addWeightsUpdate(weightsUpdate: SparseNumVector): Unit = {
    counts += 1
    weightsUpdateAggregated += weightsUpdate
  }

  def getAndResetWeighsUpdate(): SparseNumVector = {
    counts = 0
    val tmp = weightsUpdateAggregated
    weightsUpdateAggregated = SparseNumVector.empty
    tmp
  }

}