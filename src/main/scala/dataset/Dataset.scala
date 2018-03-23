package dataset


import util.Label
import util.Label.Label
import util.Types.{Counts, SparseVector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.{BufferedSource, Source}
import scala.util.Random

object Dataset {

  lazy val dids: Set[Int] = didToLineIndex.keySet
  lazy val tidCounts: Map[Int, Int] = {
    println("...loading tidCounts...")
    filePaths
      .flatMap(path =>
        Source.fromFile(path)
          .getLines()
          .flatMap(line => parseLine(line)._2.keys))
      .groupBy(tid => tid)
      .mapValues(_.size)
  }
  lazy val didToLabel: Map[Int, Label] = {
    println("...loading didToLabel...")
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
  private lazy val numLinesPerFile: List[Int] = filePaths.map(Source.fromFile).map(_.getLines.count(!_.isEmpty))
  private lazy val startingIndexPerFile: List[Int] = {
    println("...loading startingIndexPerFile...")
    val numFiles = numLinesPerFile.size
    0 :: (1 until numFiles).map(i => numLinesPerFile.dropRight(numFiles - i).sum).toList
  }
  private lazy val didToLineIndex: Map[Int, Int] = {
    println("...loading didToLineIndex...")
    filePaths.map(Source.fromFile).flatMap(_.getLines.map(_.split(" ").head.toInt)).zipWithIndex.toMap
  }
  private val dataPath = "data/"
  private val filePaths: List[String] = (0 until 4).map(i => dataPath + filename(i)).toList

  def load(): Future[Unit] = {
    Future {
      didToLabel
      didToLineIndex
    }

  }

  def samples(withReplacement: Boolean = false): Stream[(SparseVector, Label, Counts)] = {
    val docIndicesIndexSeq = dids.toIndexedSeq

    def didToOutput(did: Int): (SparseVector, Label, Counts) = {
      val feature = getFeature(did)
      val tidCountsFiltered = feature.map { case (k, _) => k -> tidCounts.withDefaultValue(0)(k) }
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

  def getFeature(did: Int): SparseVector = {
    if (!didToLineIndex.contains(did)) {
      return Map.empty
    }
    val lineIndex = didToLineIndex(did)
    require(lineIndex < numLinesPerFile.sum)

    val fileIndex = startingIndexPerFile
      .zipWithIndex.tail
      .find(_._1 > lineIndex)
      .map(_._2)
      .getOrElse(startingIndexPerFile.size) - 1
    val lineInFileIndex = lineIndex - startingIndexPerFile(fileIndex)
    val source: BufferedSource = Source.fromFile(filePaths(fileIndex))
    val line = source.getLines.drop(lineInFileIndex).next

    parseLine(line)._2
  }

  def getLabel(index: Int): Label = {
    didToLabel(index)
  }

  private def parseLine(line: String): (Int, SparseVector) = {
    val lineSplitted = line.split(" ").map(_.trim).filterNot(_.isEmpty).toList
    val did: Int = lineSplitted.head.toInt
    did -> pairsToDocument(lineSplitted.tail)
  }

  private def pairsToDocument(lineSplitted: List[String]): SparseVector = {
    lineSplitted.map(e => {
      val pair: List[String] = e.split(":").map(_.trim).toList
      pair.head.toInt -> pair.tail.head.toDouble
    }).toMap
  }

  private def filename(i: Int) = s"lyrl2004_vectors_test_pt$i.dat"

}
