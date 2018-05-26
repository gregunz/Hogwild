package model

import dataset.Dataset

case class StoppingCriteria(dataset: Dataset, earlyStopping: Int, minLoss: Double) {
  private lazy val (someFeatures, someLabels) = dataset.testSet.unzip

  private var bestWeights = SparseNumVector.empty[Double]
  private var bestLoss = Double.MaxValue
  private var bestAccuracy = 0d
  private var timesWithoutImproving = 0

  def compute(svm: SVM, displayLoss: Boolean): (Double, Double) = {
    val (loss, accuracy) = svm.lossAndAccuracy(someFeatures, someLabels, dataset.inverseTidCountsVector)
    if (displayLoss) {
      println(s"[LOSS = $loss][ACCURACY = ${accuracy * 100} %]")
    }
    if (loss < bestLoss) {
      bestLoss = loss
      bestAccuracy = accuracy
      bestWeights = svm.weights
    } else {
      println("[LOSS] my loss did not improve! :'(  (crying alone)")
      timesWithoutImproving += 1
    }
    loss -> accuracy
  }

  def shouldStop: Boolean = {
    bestLoss < minLoss || timesWithoutImproving > earlyStopping
  }

  def getWeights: SparseNumVector[Double] = bestWeights

  def getLoss: Double = bestLoss

  def getAccuracy: Double = bestAccuracy
}