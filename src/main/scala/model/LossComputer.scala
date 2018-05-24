package model

import dataset.Dataset

case class LossComputer(interval: Int, dataset: Dataset) {
  lazy val (someFeatures, someLabels) = dataset.validationSet.unzip
  var time: Long = System.currentTimeMillis()

  def compute(svm: SVM, displayLoss: Boolean = true): Double = {
    val loss = svm.loss(
      someFeatures,
      someLabels,
      dataset.tidCounts
    )
    if (displayLoss) {
      val now = System.currentTimeMillis()
      val duration = ((now - time) / 100d).toInt / 10d
      time = now
      println(s"[LOSS][$duration sec] $loss")
    }
    loss
  }

}
