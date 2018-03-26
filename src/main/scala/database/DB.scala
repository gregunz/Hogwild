package database

import java.io.File

import database.DB.exec
import database.models.{CountTable, FeatureTable, WeightTable}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery
import utils.Types.{Counts, SparseVector}

import scala.concurrent._
import scala.concurrent.duration._


object DB {

  println("Reaching database...")
  private val db = Database.forConfig("mysql")
  private val featureTable = TableQuery[FeatureTable]
  private val weightTable = TableQuery[WeightTable]
  private val countTable = TableQuery[CountTable]
  private val tables = Seq(featureTable, weightTable, countTable)
  private val tableNames = Seq("feature", "weight", "count")
  // private val tableFiles = Seq("features", "weights", "tid_counts")

  setupDB()

  lazy val tidCounts: Counts = {
    println("[DB]: Fetching tidCounts...")
    exec(queryTidCounts.result).map(c => c.tid -> c.count).toMap
  }

  lazy val dids: Set[Int] = {
    println("[DB]: Fetching dids...")
    exec({
      for {f <- featureTable} yield f.did
    }.distinct.result).toSet
  }

  def setupDB(): Unit = {
    //tables.zip(tableNames).zip(tableFiles).foreach{ case ((table, name), file) =>
    tables.zip(tableNames).foreach{ case (table, name) =>
      createTableIfNotExist(table, name)
      /*
      if(file.nonEmpty) {
        println(s"[DB]: LOADING Table $name")
        val path = new File(s"data/$file.csv").getAbsolutePath
        exec(insertFromCSV(path, name))
      }
      */
    }

    println("LOADING DONE!")
  }

  private def createTableIfNotExist[U, T <: Table[U]](table: TableQuery[T], name: String): Unit = {
    val tableExist = exec(MTable.getTables(name)).nonEmpty
    if (!tableExist) {
      println(s"[DB]: CREATING Table $name")
      weightTable
      table
      exec(table.schema.create)
    }
  }

  def getFeature(did: Int): SparseVector = {
    exec(queryFeature(did).result).toMap
  }

  private def exec[T](action: DBIO[T]): T = {
    Await.result(db.run(action), 15.minutes)
  }

  private def insertFromCSV(path: String, into: String): DBIO[Int] = {
    val mysqlPath = path.replace("\\", "\\\\") // windows related issue with paths
    throw new NotImplementedError("This is not yet working")
    sqlu"LOAD DATA LOCAL INFILE ${mysqlPath} INTO TABLE ${into} FIELDS TERMINATED BY ','  LINES TERMINATED BY '\n'"
  }

  private def queryFeature(did: Int) = for {
    f <- featureTable if f.did === did
  } yield f.tid -> f.weight

  private def queryTidCounts = countTable//.groupBy(_.tid).map { case (tid, ls) => tid -> ls.size }

}
