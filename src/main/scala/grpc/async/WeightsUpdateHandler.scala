package grpc.async

import model.SparseNumVector

case class WeightsUpdateHandler(broadcastInterval: Int) {
  private var weightsUpdateAggregated: SparseNumVector[Double] = SparseNumVector.empty
  private var counter = 0

  def addWeightsUpdate(weightsUpdate: SparseNumVector[Double]): Unit = {
    counter += 1
    weightsUpdateAggregated += weightsUpdate
  }

  def shouldBroadcast: Boolean = counter >= broadcastInterval

  def getAndResetWeightsUpdate(): SparseNumVector[Double] = {
    counter = 0
    val tmp = weightsUpdateAggregated
    weightsUpdateAggregated = SparseNumVector.empty
    tmp
  }

}