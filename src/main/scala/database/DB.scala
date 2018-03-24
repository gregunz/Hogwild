package database

import slick.dbio.Effect.{Read, Schema, Write}

import util.Types.{Counts, SparseVector}


import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction}

import scala.io.Source

object DB extends App {


  private def parseLine(line: String): (Int, SparseVector) = {
    val lineSplitted = line.split(" ").map(_.trim).filterNot(_.isEmpty).toList
    val did: Int = lineSplitted.head.toInt
    did -> pairsToDocument(lineSplitted.tail)
  }

  private def pairsToDocument(lineSplitted: List[String]): SparseVector = {
    lineSplitted.map(e => {
      val pair: List[String] = e.split(":").map(_.trim).toList
      pair.head.toInt -> pair.tail.head.toDouble
    }).toMap
  }

  lazy val featureTable = TableQuery[FeatureTable]

  val createTableAction: FixedSqlAction[Unit, NoStream, Schema] = featureTable.schema.create

  def insertAlbumsAction(lines:Seq[String]): FixedSqlAction[Option[Int], NoStream, Write] = featureTable ++=
    lines.map(line => parseLine(line)).flatMap(x => x._2.map(y => FeatureSchema(x._1, y._1, y._2)))

  def insertFile(filename:String): Unit = {
    Source.fromFile(filename).getLines().sliding(1000, 1000).foreach(lines => exec(insertAlbumsAction(lines)))
  }

  val selectAlbumsActions: FixedSqlStreamingAction[Seq[FeatureSchema], FeatureSchema, Read] = featureTable.result

  val query = for {
    f <- featureTable if f.did === 1523l
  } yield f.tid -> f.weight

  private val db = Database.forConfig("scalaxdb")

  private def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 1200.seconds)

  exec(createTableAction)
  println("Loading start")
  val startTimeL = System.currentTimeMillis()
  insertFile(s"data/lyrl2004_vectors_test_pt0.dat")
  println("Final loading ended after "+(System.currentTimeMillis() - startTimeL)/1000+" seconds")
  //exec(selectAlbumsActions)
  val startTimeQ = System.currentTimeMillis()
  println(exec(query.result).toMap)
  println("Time to answer 1 query: "+(System.currentTimeMillis() - startTimeQ)+" milli-seconds")


}
