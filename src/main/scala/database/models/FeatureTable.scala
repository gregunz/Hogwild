package database.models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

class FeatureTable(tag: Tag) extends Table[FeatureSchema](tag, "feature") {

  def * : ProvenShape[FeatureSchema] = (did, tid, weight) <> (FeatureSchema.tupled, FeatureSchema.unapply)

  def weight: Rep[Double] = column[Double]("weight")

  def pk = primaryKey("pk_did_tid", (did, tid))

  def did: Rep[Int] = column[Int]("did")

  def tid: Rep[Int] = column[Int]("tid")
}
