package dataset

import model.SparseNumVector
import utils.Label
import utils.Label.Label
import utils.Types.{Counts, TID}

import scala.io.Source
import scala.util.Random

case class Dataset(dataPath: String, onlySamples: Boolean) {

  lazy val didSet: Set[Int] = load("didSet") {
    features.keySet
  }
  lazy val tidCounts: Counts = load("tidCounts") {
    features.flatMap(_._2.tids.toSeq)
      .groupBy(tid => tid)
      .mapValues(_.size)
  }
  lazy val labels: Map[Int, Label] = load("labels") {
    val labelPath = dataPath + "rcv1-v2.topics.qrels"
    val labelOfInterest = "CCAT"
    Source.fromFile(labelPath)
      .getLines
      .map { line =>
        line.split(" ").filterNot(_.isEmpty).take(2).toList match {
          case label :: id :: Nil => id.toInt -> (label == labelOfInterest)
          case _ => throw new IllegalStateException("label file is corrupted")
        }
      }
      .toList
      .groupBy(_._1)
      .mapValues { v => Label(v.exists(_._2)) }
  }
  lazy val features: Map[Int, SparseNumVector[Double]] = load("features") {
    filePaths
      .flatMap { path =>
        Source.fromFile(path)
          .getLines()
          .map { line => parseLine(line) }
      }.toMap
  }

  def filePaths: List[String] = {
    if (onlySamples) {
      List(dataPath + "samples.dat")
    } else {
      (0 until 4).map(i => dataPath + filename(i)).toList
    }
  }

  private def filename(i: Int) = s"lyrl2004_vectors_test_pt$i.dat"

  def fullLoad(): Dataset = load("dataset") {
    labels
    features
    tidCounts
    didSet
    this
  }

  private def load[T](name: String)(toLoad: => T): T = {
    println(s"...loading $name...")
    val toReturn = toLoad
    println(s"$name loaded.")
    toReturn
  }

  def getSubset(n: Int): IndexedSeq[(SparseNumVector[Double], Label)] = {
    val someDids: IndexedSeq[TID] = didSet.take(n).toIndexedSeq
    val someFeatures: IndexedSeq[SparseNumVector[Double]] = someDids.map(features)
    val someLabels: IndexedSeq[Label] = someDids.map(labels)
    someFeatures zip someLabels
  }

  def samples(withReplacement: Boolean = false): Stream[Int] = {
    val docIndicesIndexSeq = didSet.toIndexedSeq
    if (!withReplacement) {
      Stream.continually {
        Random.shuffle(docIndicesIndexSeq).toStream
      }.flatten
    } else {
      Stream.continually {
        val randomIndex = Random.nextInt(docIndicesIndexSeq.size)
        val did = docIndicesIndexSeq(randomIndex)
        did
      }
    }
  }

  def getFeature(did: Int): SparseNumVector[Double] = {
    features(did)
  }

  def getLabel(index: Int): Label = {
    labels(index)
  }

  private def parseLine(line: String): (Int, SparseNumVector[Double]) = {
    val lineSplitted = line.split(" ").map(_.trim).filterNot(_.isEmpty).toList
    val did: Int = lineSplitted.head.toInt
    did -> pairsToVector(lineSplitted.tail)
  }

  private def pairsToVector(lineSplitted: List[String]): SparseNumVector[Double] = {
    SparseNumVector(
      lineSplitted.map(e => {
        val pair: List[String] = e.split(":").map(_.trim).toList
        pair.head.toInt -> pair.tail.head.toDouble
      }).toMap)
  }

}
