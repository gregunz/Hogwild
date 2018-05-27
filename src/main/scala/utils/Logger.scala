package utils

import java.util.Calendar

import scala.collection.immutable.Queue

case class Logger(verbosityLevel: Int, logFun: String => Unit = println) {
  require(verbosityLevel >= Logger.minLevel)

  private var logs: Queue[(String, Boolean)] = Queue.empty

  def alwaysLog(s: String): Unit = {
    log(Logger.minLevel)(s)
  }

  def log(level: Int)(s: String): Unit = {
    val shouldLog = level <= verbosityLevel
    store(s, shouldLog)
    if (shouldLog) {
      val time = Calendar.getInstance.getTime.toString
      logFun(s"[$time] $s")
    }
  }

  private def store(s: String, shouldLog: Boolean): Unit = {
    logs = logs.enqueue(s -> shouldLog)
  }

  def load[T](level: Int)(name: String)(toLoad: => T): T = {
    log(2)(s"loading $name...")
    val toReturn = toLoad
    log(2)(s"$name loaded.")
    toReturn
  }

  def getAllLogs: Iterator[String] = logs.iterator.map(_._1)

  def getFilteredLogs: Iterator[String] = logs.iterator.filter(_._2).map(_._1)
}

object Logger {
  val minLevel = 0

  def minimal: Logger = Logger(minLevel)
}