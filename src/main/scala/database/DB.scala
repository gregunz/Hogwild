package database

import slick.dbio.Effect.{ Read, Schema, Write }

import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ ProvenShape, TableQuery }
import slick.sql.{ FixedSqlAction, FixedSqlStreamingAction }

object DB extends App {

  lazy val featureTable = TableQuery[FeatureTable]

  val createTableAction: FixedSqlAction[Unit, NoStream, Schema] = featureTable.schema.create

  val insertAlbumsAction: FixedSqlAction[Option[Int], NoStream, Write] = featureTable ++= Seq(
    FeatureSchema(0, 0, 33.33),
    FeatureSchema(1, 0, 34.33),
    FeatureSchema(2, 0, 35.33),
    FeatureSchema(1, 2, 36.33),
    FeatureSchema(1, 10, 37.33),
    FeatureSchema(2, 10, 38.33),
  )

  val selectAlbumsActions: FixedSqlStreamingAction[Seq[FeatureSchema], FeatureSchema, Read] = featureTable.result

  val query = for {
    f <- featureTable if f.did === 1l
  } yield f.tid -> f.weight

  private val db = Database.forConfig("scalaxdb")

  private def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2.seconds)

  exec(createTableAction)
  exec(insertAlbumsAction)
  exec(selectAlbumsActions).foreach(println)
  println(exec(query.result).toMap)


}
