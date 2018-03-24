package database

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

class FeatureTable(tag: Tag) extends Table[FeatureSchema](tag, "FEATURE") {

  def * : ProvenShape[FeatureSchema] = (did, tid, weight) <> (FeatureSchema.tupled, FeatureSchema.unapply)

  def did: Rep[Long] = column[Long]("DID")

  def tid: Rep[Long] = column[Long]("TID")

  def weight: Rep[Double] = column[Double]("WEIGHT")
}
