package dataset

import scala.io.{BufferedSource, Source}

object Dataset {
  type Data = Any

  def load: Data = {
    val path = "data/"
    def filename(i: Int) = s"lyrl2004_vectors_test_pt$i.dat"
    val filePaths = (0 until 4).map(i => path + filename(i))
    var n = 0
    val data: BufferedSource = Source.fromFile(filePaths.head)
    for (line <- data.getLines if n < 100) {
      val cols = line.split(" ").map(_.trim)
      // do whatever you want with the columns here
      n += 1
      println(s"${cols(0)}|${cols(1)}|${cols(2)}|${cols(3)}")
    }
    data.close
  }
}
