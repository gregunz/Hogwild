package model

import dataset.Dataset
import utils.{Interval, Logger, Utils}

case class StoppingCriteria(logger: Logger, dataset: Dataset, earlyStopping: Int, minLoss: Double, interval: Interval) {

  private var bestWeights = SparseNumVector.empty[Double]
  private var bestLoss = Double.MaxValue
  private var losses: Seq[Double] = Seq.empty
  private var bestAccuracy = 0d
  private var accuracies: Seq[Double] = Seq.empty
  private var timesWithoutImproving = 0

  def compute(svm: SVM, displayLoss: Boolean): (Double, Double) = {
    val (loss, accuracy) = svm.lossAndAccuracy(dataset.testSet, dataset.inverseTidCountsVector)
    losses +:= loss
    accuracies +:= accuracy
    if (displayLoss) {
      logger.log(1)(s"[LOSS = $loss][ACCURACY = ${accuracy * 100} %]")
    }
    if (loss < bestLoss) {
      bestLoss = loss
      bestAccuracy = accuracy
      bestWeights = svm.weights
    } else {
      timesWithoutImproving += 1
      logger.log(1)(s"[LOSS] my loss did not improve! :'(  (crying alone) ($timesWithoutImproving time(s))")
    }
    loss -> accuracy
  }

  def shouldStop: Boolean = {
    bestLoss < minLoss || timesWithoutImproving > earlyStopping
  }

  def getWeights: SparseNumVector[Double] = bestWeights

  def getLoss: Double = bestLoss

  def getAccuracy: Double = bestAccuracy

  def export(): Unit = {
    Iterator(
      ("weights.csv", bestWeights.toMap.iterator.map { case (tid, value) => s"$tid, $value" }),
      ("stats.csv", (losses zip accuracies).map { case (l, a) => s"$l, $a" }.reverseIterator.map(_.toString))
    ).foreach { case (filename, lines) =>
      Utils.upload(filename, lines)
    }

    Utils.upload("filtered_logs.txt", logger.getFilteredLogs)
    Utils.upload("all_logs.txt", logger.getAllLogs)
  }

}