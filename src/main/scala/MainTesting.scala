import dataset.Dataset
import model.SparseNumVector

import scala.collection.mutable
import scala.io.Source

object MainTesting extends App {

  val tids = IndexedSeq(1)
  while(true){
    val time = System.currentTimeMillis()
    val dataset = Dataset("data/", onlySamples = false)
    val dids = (0 to 781265).toSet.take(100000)
    val it = dataset.filePaths.take(4).iterator.flatMap{ path =>
      Source.fromFile(path).getLines
    }.zipWithIndex
      .filter( a => dids(a._2))
      .map(_._1)
      .toList

    println("iterator created")
    //println(it.size)

    it.par.map(parseLine).toMap

    println(s"over in ${secondPassed(time)}")
  }

  private def secondPassed(time: Long): String = {
    val dif = System.currentTimeMillis() - time
    dif / 1000 + " sec"
  }

  private def parseLine(line: String): (Int, SparseNumVector[Double]) = {
    val lineSplitted = line.split(" ").map(_.trim).filter(_.nonEmpty)
    val did: Int = lineSplitted(0).toInt
    did -> pairsToVector(lineSplitted.drop(1))
  }

  private def pairsToVector(lineSplitted: Array[String]): SparseNumVector[Double] = {
    val vector = lineSplitted.map{ e =>
      val pair = e.split(":").map(_.trim)
      pair(0).toInt -> pair(1).toDouble
    }
    SparseNumVector(vector.toMap)
  }

}
