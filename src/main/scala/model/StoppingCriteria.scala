package model

import dataset.Dataset
import utils.Label.Label
import utils.{Interval, Logger, Utils}

import scala.collection.immutable.Queue

case class StoppingCriteria(logger: Logger, dataset: Dataset, earlyStopping: Int, minLoss: Double, interval: Interval) {

  private var bestWeights = SparseNumVector.empty[Double]
  private var bestValLoss = Double.MaxValue
  private var bestValAccuracy = 0d

  private var timesWithoutImproving = 0

  private var trainLosses = Queue.empty[Double]
  private var trainAccuracies = Queue.empty[Double]
  private var trainBatchSizes = Queue.empty[Int]

  private var trainLossesBatch = Queue.empty[Double]
  private var trainAccuraciesBatch = Queue.empty[Double]

  private var valLosses = Queue.empty[Double]
  private var valAccuracies = Queue.empty[Double]

  def computeStats(svm: SVM, displayStats: Boolean): (Double, Double) = {

    val batchSize = trainLossesBatch.size
    if (batchSize > 0) {

      trainBatchSizes = trainBatchSizes.enqueue(batchSize)
      val trainLoss = trainLossesBatch.sum / batchSize
      trainLosses = trainLosses.enqueue(trainLoss)
      val trainAcc = trainAccuraciesBatch.sum / batchSize
      trainAccuracies = trainAccuracies.enqueue(trainAcc)

      if (displayStats) {
        logger.log(1)(s"[TRAIN][LOSS = $trainLoss][ACCURACY = ${trainAcc * 100} %][BATCH_SIZE = $batchSize]")
      }

      trainLossesBatch = Queue.empty
      trainAccuraciesBatch = Queue.empty
    }

    val (valLoss, valAcc) = svm.lossAndAccuracy(dataset.testSet, dataset.inverseTidCountsVector)
    valLosses = valLosses.enqueue(valLoss)
    valAccuracies = valAccuracies.enqueue(valAcc)

    if (displayStats) {
      logger.log(1)(s"[VAL][LOSS = $valLoss][ACCURACY = ${valAcc * 100} %]")
    }

    if (valLoss < bestValLoss) {
      bestValLoss = valLoss
      bestValAccuracy = valAcc
      bestWeights = svm.weights
      timesWithoutImproving = 0
    } else {
      timesWithoutImproving += 1
      logger.log(2)(s"[LOSS] my loss did not improve! :'(  (crying alone) ($timesWithoutImproving time(s))")
    }
    valLoss -> valAcc
  }

  def addTrainSample(svm: SVM, feature: SparseNumVector[Double], label: Label): Unit = {
    val (trainLoss, trainAcc) = svm.lossAndAccuracy(Seq(feature -> label), dataset.inverseTidCountsVector)
    trainLossesBatch = trainLossesBatch.enqueue(trainLoss)
    trainAccuraciesBatch = trainAccuraciesBatch.enqueue(trainAcc)
  }

  def shouldStop: Boolean = {
    bestValLoss < minLoss || timesWithoutImproving >= earlyStopping
  }

  def getWeights: SparseNumVector[Double] = bestWeights

  def getLoss: Double = bestValLoss

  def getAccuracy: Double = bestValAccuracy

  def export(): Unit = {
    Iterator(
      ("weights.csv", bestWeights.toMap.iterator.map { case (tid, value) => s"$tid, $value" }),
      ("val_stats.csv", (valLosses zip valAccuracies).map { case (l, a) => s"$l, $a" }.iterator.map(_.toString)),
      ("train_stats.csv", (trainLosses zip trainAccuracies zip trainBatchSizes).map { case ((l, a), b) => s"$l, $a, $b" }.iterator.map(_.toString))
    ).foreach { case (filename, lines) =>
      Utils.upload(filename, lines, andPrint = true)
    }

    Utils.upload("filtered_logs.txt", logger.getFilteredLogs, andPrint = false)
    Utils.upload("all_logs.txt", logger.getAllLogs, andPrint = false)

    logger.log(2)(s"Convergence time is ${logger.timeFromStart}")
  }

}