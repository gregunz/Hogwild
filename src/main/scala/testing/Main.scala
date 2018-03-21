package testing

import dataset.Dataset
import dataset.Dataset.numLinesPerFile

object Main extends App {

  //print(Int.MaxValue)
  println(
    Dataset.numLinesPerFile,
    Dataset.startingIndexPerFile,
  )

  val map = Dataset.lineIndexToDocIndex

  println(map.values.toList.sorted.take(100))
}
