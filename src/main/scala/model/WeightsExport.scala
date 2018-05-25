package model

import java.io.{BufferedWriter, File, FileWriter}

object WeightsExport {

  import sys.process._

  private val outputDirPath = "/output"
  private val weightsFilename = "weights.csv"
  private val weightsFilepath = s"$outputDirPath/$weightsFilename"

  private def createDir(dirPath: String): Boolean = {
    val dir = new File(dirPath)
    !dir.exists && dir.mkdirs
  }

  private def writeWeightsToFile(dirPath: String, filename: String, weights: SparseNumVector[Double]): Unit = {
    createDir(dirPath)
    val file = new File(s"$dirPath/$filename")
    val bw = new BufferedWriter(new FileWriter(file))
    weights.toMap.foreach{ case (tid, value) =>
      val toWrite = s"$tid,$value\n"
      bw.write(toWrite)
    }
    bw.close()
  }

  def uploadWeightsAndGetLink(weights: SparseNumVector[Double]): Unit = {
    writeWeightsToFile(outputDirPath, weightsFilename, weights)
    println(s"curl --upload-file $weightsFilepath https://transfer.sh/weights.csv" !!)
  }

}
