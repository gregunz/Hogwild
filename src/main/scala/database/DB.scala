package database

import slick.dbio.Effect.Schema
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery
import slick.sql.FixedSqlAction
import utils.Types.{Counts, SparseVector}

import scala.concurrent._
import scala.concurrent.duration._
import java.io.File


object DB {

  println("Reaching database...")
  private val db = Database.forConfig("mysql")
  private val featureTable = TableQuery[FeatureTable]

  val isReady: Boolean = exec(MTable.getTables("feature")).nonEmpty
  if(isReady) {
    println(">> Database is ready <<")
  } else {
    setupDB()
  }

  lazy val tidCounts: Counts = {
    println("[DB]: Fetching tidCounts...")
    exec(queryTidCounts.result).toMap
  }
  lazy val dids: Set[Int] = {
    println("[DB]: Fetching dids...")
    exec({for {f <- featureTable} yield f.did}.distinct.result).toSet
  }

  def setupDB(): Unit = {
    if (!isReady) {
      println(">> SETTING UP DATABASE << (this takes some time)")
      val createTableAction: FixedSqlAction[Unit, NoStream, Schema] = featureTable.schema.create
      exec(createTableAction)

      // inserting features
      val path = new File("data/output2.csv").getAbsolutePath
      exec(insertFromCSV(path, "feature"))
      println("LOADING DONE!")
    }
  }

  def getFeature(did: Int): SparseVector = {
    exec(queryFeature(did).result).toMap
  }

  private def exec[T](action: DBIO[T]): T = {
    Await.result(db.run(action), 15.minutes)
  }

  private def queryFeature(did: Int) = for {
    f <- featureTable if f.did === did
  } yield f.tid -> f.weight

  private def queryTidCounts = featureTable.groupBy(_.tid).map{ case(tid, ls) => tid -> ls.size }

  private def insertFromCSV(path: String, into: String): DBIO[Int] = {
    val mysqlPath = path.replace("\\", "\\\\")
    throw new NotImplementedError("This is not yet working")
    sqlu"LOAD DATA LOCAL INFILE ${mysqlPath} INTO TABLE ${into} FIELDS TERMINATED BY ','  LINES TERMINATED BY '\n'"
  }

}
