import dataset.Dataset

object Testing extends App {

  val d = Dataset("data/")
  println(d.tids.toSet diff d.inverseTidCountsVector.keys)
  println(d.inverseTidCountsVector.keys diff d.tids.toSet)

}
