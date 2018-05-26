import dataset.Dataset

object MainTesting extends App {

  val d = Dataset("data/").getReady(true)

  println(d.getSample)
  println(d.testSet.size, d.testSet.take(2))
  println(d.inverseTidCountsVector.toMap.size, d.inverseTidCountsVector)

}
