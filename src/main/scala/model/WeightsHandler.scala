package model

class WeightsHandler {
  private var weightsUpdateAggregated: SparseNumVector = SparseNumVector.empty

  def addWeightsUpdate(weightsUpdate: SparseNumVector): Unit = {
    weightsUpdateAggregated += weightsUpdate
  }

  def getAndResetWeighsUpdate(): SparseNumVector = {
    val tmp = weightsUpdateAggregated
    weightsUpdateAggregated = SparseNumVector.empty
    tmp
  }

}
