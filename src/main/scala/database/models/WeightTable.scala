package database.models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

class WeightTable(tag: Tag) extends Table[WeightSchema](tag, "weight") {

  def * : ProvenShape[WeightSchema] = (tid, weight) <> (WeightSchema.tupled, WeightSchema.unapply)

  def weight: Rep[Double] = column[Double]("weight")

  def tid: Rep[Int] = column[Int]("tid", O.PrimaryKey)
}
