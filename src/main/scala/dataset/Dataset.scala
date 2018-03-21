package dataset

import scala.io.{BufferedSource, Source}

object Dataset {

  type Data = Map[Int, Document]
  type Document = Map[Int, Float]
  type Label = String

  private val dataPath = "data/"

  val filePaths: List[String] = (0 until 4).map(i => dataPath + filename(i)).toList

  lazy val numLinesPerFile: List[Int] = filePaths.map(Source.fromFile).map(_.getLines().size)

  lazy val startingIndexPerFile: List[Int] = {
    val numFiles = numLinesPerFile.size
    0 :: (1 until numFiles).map(i => numLinesPerFile.dropRight(numFiles - i).sum).toList
  }

  lazy val docIndexToLineIndex: Map[Int, Int] = {
    filePaths.map(Source.fromFile).flatMap(_.getLines().map(_.split(" ").head.toInt)).zipWithIndex.toMap
  }

  lazy val lineIndexToDocIndex: Map[Int, Int] = {
    docIndexToLineIndex.map{case(k, v) => v -> k}
  }

  def load: Data = {

    println(numLinesPerFile)

    val source: BufferedSource = Source.fromFile(filePaths.head)

    val data = source.getLines().map { line =>

      val line_splitted = line.split(" ").map(_.trim).filter(!_.isEmpty).toList
      val did: Int = line.head.toInt
      did -> toDocument(line_splitted)

    }.toMap

    source.close
    data
  }

  private def toDocument(line: List[String]): Document = {
    line.tail.map(e => {
      val pair: List[String] = e.split(":").map(_.trim).toList
      pair.head.toInt -> pair.tail.head.toFloat
    }).toMap
  }

  def get(index: Int, isDocumentIndex: Boolean = false): Document = {
    ???
  }

  def getLabel(index: Int, isDocumentIndex: Boolean = false): Label = {
    ???
  }

  private def filename(i: Int) = s"lyrl2004_vectors_test_pt$i.dat"
}
