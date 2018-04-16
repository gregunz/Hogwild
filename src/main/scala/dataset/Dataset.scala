package dataset

import model.SparseNumVector
import utils.Label
import utils.Label.Label
import utils.Types.{Counts, TID}

import scala.io.Source
import scala.util.Random

object Dataset {

  private final val dataPath = "data/"
  private final val filePaths: List[String] = {
    // at the moment, can hold the whole data in memory => subset of data only
    //(0 until 4).map(i => dataPath + filename(i)).toList
    List(dataPath + filename(0))
  }

  lazy val didSet: Set[TID] = features.keySet
  lazy val tidCounts: Counts = {
    println("...loading tidCounts...")
    features.flatMap(_._2.values.keys)
      .groupBy(tid => tid)
      .mapValues(_.size)
  }
  lazy val labels: Map[TID, Label] = {
    println("...loading labels...")
    val labelPath = dataPath + "rcv1-v2.topics.qrels"
    val labelOfInterest = "CCAT"
    Source.fromFile(labelPath)
      .getLines
      .map { line =>
        line.split(" ").toList.filterNot(_.isEmpty).take(2) match {
          case label :: id :: Nil => id.toInt -> (label == labelOfInterest)
          case _ => throw new IllegalStateException("label file is corrupted")
        }
      }
      .toList
      .groupBy(_._1)
      .mapValues { v => Label(v.exists(_._2)) }
  }
  lazy val features: Map[TID, SparseNumVector] = {
    println("...loading features...")
    filePaths
      .flatMap { path =>
        Source.fromFile(path)
          .getLines()
          .map { line => parseLine(line) }
      }.toMap
  }

  def load(): Unit = {
    labels
    features
    tidCounts
  }

  def samples(withReplacement: Boolean = false): Stream[(SparseNumVector, Label, Map[TID, Int])] = {
    val docIndicesIndexSeq = didSet.toIndexedSeq

    def didToOutput(did: Int): (SparseNumVector, Label, Map[TID, Int]) = {
      val feature = getFeature(did)
      val tidCountsFiltered = feature.values.map { case (k, _) => k -> tidCounts(k) }
      (feature, getLabel(did), tidCountsFiltered)
    }

    if (!withReplacement) {
      Stream.continually {
        Random.shuffle(docIndicesIndexSeq).toStream.map(didToOutput)
      }.flatten
    } else {
      Stream.continually {
        val randomIndex = Random.nextInt(docIndicesIndexSeq.size)
        val did = docIndicesIndexSeq(randomIndex)
        didToOutput(did)
      }
    }
  }

  def getFeature(did: Int): SparseNumVector = {
    features(did)
  }

  def getLabel(index: Int): Label = {
    labels(index)
  }

  private def parseLine(line: String): (Int, SparseNumVector) = {
    val lineSplitted = line.split(" ").map(_.trim).filterNot(_.isEmpty).toList
    val did: Int = lineSplitted.head.toInt
    did -> pairsToVector(lineSplitted.tail)
  }

  private def pairsToVector(lineSplitted: List[String]): SparseNumVector = {
    SparseNumVector(
      lineSplitted.map(e => {
        val pair: List[String] = e.split(":").map(_.trim).toList
        pair.head.toInt -> pair.tail.head.toDouble
      }).toMap)
  }

  private def filename(i: Int) = s"lyrl2004_vectors_test_pt$i.dat"

}
