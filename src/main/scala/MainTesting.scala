import dataset.Dataset
import model.SparseNumVector
import utils.Label
import utils.Label.Label

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

object MainTesting extends App {

  Dataset("data/").getReady(true)

}
