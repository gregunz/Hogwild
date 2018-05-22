package utils

import grpc.async.Worker.RemoteWorker
import grpc.async.{Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}

import scala.util.Try

trait Mode {
  def run(): Unit
  def printMode(mode: Mode): Unit = println(s"Getting ready!! ($mode)")
}

case class SyncWorkerMode(serverIp: String, serverPort: Int) extends Mode {
  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(port: Int) extends Mode {
  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(port: Int, interval: Int, worker: Option[RemoteWorker]) extends Mode {
  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }
}

object DefaultMode extends Mode {
  def run(): Unit = println("arguments mismatch")
}

object Mode {
  def apply(options: Map[String, String]): Mode = {
    Try {
      options("mode") match {
        case "sync" if options("type") == "worker" =>
          options("server-ip:server-port").split(":").toList match {
            case ip :: port :: Nil => SyncWorkerMode(ip, port.toInt)
          }
        case "sync" if options("type") == "coord" =>
          SyncCoordinatorMode(options("port").toInt)
        case "async" =>
          val someWorker = options.get("worker-ip:worker-port")
            .map(_.split(":").toList match {
              case ip :: port :: Nil => RemoteWorker(ip, port.toInt)
            })
          AsyncWorkerMode(options("port").toInt, options("interval").toInt, someWorker)
        case _ => DefaultMode
      }
    }.getOrElse(DefaultMode)
  }
}