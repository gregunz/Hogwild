package grpc

import dataset.Dataset
import model.{SVM, SparseNumVector}

case class WeightsUpdateHandler(interval: Int, dataset: Dataset) {
  private var weightsUpdateAggregated: SparseNumVector[Double] = SparseNumVector.empty
  private var counter = 0

  lazy val (someFeatures, someLabels) = dataset.validationSet.unzip
  var time: Long = System.currentTimeMillis()

  def addWeightsUpdate(weightsUpdate: SparseNumVector[Double]): Unit = {
    counter += 1
    weightsUpdateAggregated += weightsUpdate
  }

  def showLoss(svm: SVM): Unit = {
    val loss = svm.loss(
      someFeatures,
      someLabels,
      dataset.tidCounts
    )
    val now = System.currentTimeMillis()
    val duration = ((now - time) / 100d).toInt / 10d
    time = now
    println(s"[LOSS][$duration sec] $loss")
  }

  def reachedInterval: Boolean = counter >= interval

  def resetInterval(): Unit = counter = 0

  def resetWeightsUpdate(): Unit = weightsUpdateAggregated = SparseNumVector.empty

  def getAndResetWeightsUpdate(): SparseNumVector[Double] = {
      val tmp = weightsUpdateAggregated
      resetWeightsUpdate()
      tmp
  }

}