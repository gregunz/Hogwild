package grpc

import dataset.Dataset
import model.{SVM, SparseNumVector}
import utils.Interval

object WeightsUpdateHandler {
  private var weightsUpdateAggregated: SparseNumVector[Double] = SparseNumVector.empty

  def addWeightsUpdate(weightsUpdate: SparseNumVector[Double]): Unit = {
    weightsUpdateAggregated += weightsUpdate
  }

  def resetWeightsUpdate(): Unit = weightsUpdateAggregated = SparseNumVector.empty

  def getAndResetWeightsUpdate(): SparseNumVector[Double] = {
      val tmp = weightsUpdateAggregated
      resetWeightsUpdate()
      tmp
  }

}