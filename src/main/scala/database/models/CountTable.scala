package database.models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

class CountTable(tag: Tag) extends Table[CountSchema](tag, "count") {

  def * : ProvenShape[CountSchema] = (tid, count) <> (CountSchema.tupled, CountSchema.unapply)

  def count: Rep[Int] = column[Int]("count")

  def tid: Rep[Int] = column[Int]("tid", O.PrimaryKey)
}
