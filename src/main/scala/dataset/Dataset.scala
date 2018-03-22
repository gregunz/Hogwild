package dataset

import computations.SVM.{Label, Features}
import scala.io.{BufferedSource, Source}
import scala.util.Random

object Dataset {

  private val dataPath = "data/"

  val filePaths: List[String] = (0 until 4).map(i => dataPath + filename(i)).toList

  lazy val lineIndices: Set[Int] = lineIndexToDocIndex.keySet
  lazy val dids: Set[Int] = docIndexToLineIndex.keySet

  lazy val numLinesPerFile: List[Int] = filePaths.map(Source.fromFile).map(_.getLines.count(!_.isEmpty))

  lazy val startingIndexPerFile: List[Int] = {
    val numFiles = numLinesPerFile.size
    0 :: (1 until numFiles).map(i => numLinesPerFile.dropRight(numFiles - i).sum).toList
  }

  lazy val docIndexToLineIndex: Map[Int, Int] = {
    filePaths.map(Source.fromFile).flatMap(_.getLines.map(_.split(" ").head.toInt)).zipWithIndex.toMap
  }

  lazy val lineIndexToDocIndex: Map[Int, Int] = {
    docIndexToLineIndex.map{case(k, v) => v -> k}
  }

  private def filename(i: Int) = s"lyrl2004_vectors_test_pt$i.dat"

  private def parseLine(line: String): (Int, Features) = {
    val lineSplitted = line.split(" ").map(_.trim).filterNot(_.isEmpty).toList
    val did: Int = lineSplitted.head.toInt
    did -> pairsToDocument(lineSplitted.tail)
  }

  private def pairsToDocument(lineSplitted: List[String]): Features = {
    lineSplitted.map(e => {
      val pair: List[String] = e.split(":").map(_.trim).toList
      pair.head.toInt -> pair.tail.head.toDouble
    }).toMap
  }

  private def getDoc(index: Int, isDocumentIndex: Boolean = false): Features = {
    if(isDocumentIndex && !docIndexToLineIndex.contains(index)){
      return Map.empty
    }
    val lineIndex = if(isDocumentIndex) docIndexToLineIndex(index) else index
    require(lineIndex < numLinesPerFile.sum)

    val fileIndex = startingIndexPerFile
      .zipWithIndex.tail
      .find(_._1 > lineIndex)
      .map(_._2)
      .getOrElse(startingIndexPerFile.size) - 1
    val lineInFileIndex = lineIndex - startingIndexPerFile(fileIndex)
    val source: BufferedSource = Source.fromFile(filePaths(fileIndex))
    val line = source.getLines drop lineInFileIndex next

    parseLine(line)._2
  }

  private def getLabel(index: Int, isDocumentIndex: Boolean = false): Label = {
    true //TODO: should implement this
  }

  def load: Unit = {
    lineIndexToDocIndex
  }

  def sample(withReplacement: Boolean = true): (Features, Label) = {
    if(!withReplacement){
      ???
    }
    val randomIndex = Random.nextInt(lineIndices.size)
    val lineIndex = lineIndices.toIndexedSeq(randomIndex)
    getDoc(lineIndex) -> getLabel(lineIndex)
  }

}
