package utils

import java.io.{BufferedWriter, File, FileWriter}

object Utils {

  import sys.process._

  private val outputDirPath = "/output"

  def split(str: String, char: Char): (String, String) = {
    val idx = str.indexOf(char)
    val s1 = str.substring(0, idx)
    val s2 = str.substring(idx + 1)
    Iterator(s1, s2).foreach { s =>
      require(s.nonEmpty, "split ends up with empty string (\"" + str + "\" with '" + char + "')")
    }
    s1 -> s2
  }

  def upload(filename: String, lines: Iterator[String], andPrint: Boolean): Unit = {
    if (lines.hasNext) {
      writeLinesToFile(outputDirPath, filename, lines, andPrint = andPrint)
      Logger.minimal.alwaysLog(s"curl --upload-file $outputDirPath/$filename https://transfer.sh/$filename" !!)
    } else {
      Logger.minimal.alwaysLog(s"Not uploading $filename because it is empty")
    }
  }

  private def writeLinesToFile(dirPath: String, filename: String, lines: Iterator[String], sep: String = "\n", andPrint: Boolean = false): Unit = {
    createDir(dirPath)
    val file = new File(s"$dirPath/$filename")
    val bw = new BufferedWriter(new FileWriter(file))
    lines.foreach{ line =>
      if(andPrint){
        println(line)
      }
      bw.write(line + sep)
    }
    bw.close()
  }

  private def createDir(dirPath: String): Boolean = {
    val dir = new File(dirPath)
    !dir.exists && dir.mkdirs
  }

}
