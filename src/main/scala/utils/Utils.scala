package utils

import java.io.{BufferedWriter, File, FileWriter}

object Utils {
  import sys.process._
  private val outputDirPath = "/output"

  def split(str: String, char: Char): (String, String) = {
    val idx = str.indexOf(char)
    val s1 = str.substring(0, idx)
    val s2 = str.substring(idx + 1)
    Iterator(s1, s2).foreach{ s =>
      require(s.nonEmpty, "split ends up with empty string (\"" +  str + "\" with '" + char + "')")
    }
    s1 -> s2
  }

  def upload(filename: String, lines: Iterator[String], sep: String): Unit = {
    writeWeightsToFile(outputDirPath, filename, lines, sep)
    println(s"curl --upload-file $filename https://transfer.sh/weights.csv" !!)
  }

  private def writeWeightsToFile(dirPath: String, filename: String, lines: Iterator[String], sep: String): Unit = {
    createDir(dirPath)
    val file = new File(s"$dirPath/$filename")
    val bw = new BufferedWriter(new FileWriter(file))
    while(lines.hasNext){
      bw.write(lines.next + (if(lines.hasNext) sep else ""))
    }
    bw.close()
  }

  private def createDir(dirPath: String): Boolean = {
    val dir = new File(dirPath)
    !dir.exists && dir.mkdirs
  }

}
