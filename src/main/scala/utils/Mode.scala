package utils

import grpc.async.Worker.RemoteWorker
import grpc.async.{Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}
import launcher.ArgsHandler.Options

import scala.util.Try

trait Mode {
  val dataPath: String
  val samples: Boolean

  def run(): Unit

  def printMode(mode: Mode): Unit = println(s"Getting ready!! ($mode)")
}

case class SyncWorkerMode(dataPath: String, samples: Boolean, serverIp: String, serverPort: Int) extends Mode {
  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(dataPath: String, samples: Boolean, port: Int) extends Mode {
  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(dataPath: String, samples: Boolean, port: Int, interval: Int, worker: Option[RemoteWorker]) extends Mode {
  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }
}

case class DefaultMode(options: Options) extends Mode {
  val dataPath = ""
  val samples = false

  def run(): Unit = println(s"arguments mismatch ($options)")
}

object Mode {
  def apply(options: Options): Mode = {
    Try {
      val dataPath = options("data-path")
      val samples = options("samples").toInt != 0
      options("mode") match {
        case "sync" if options("type") == "worker" =>
          options("server-ip:server-port").split(":").toList match {
            case ip :: port :: Nil => SyncWorkerMode(dataPath, samples, ip, port.toInt)
          }
        case "sync" if options("type") == "coord" =>
          SyncCoordinatorMode(dataPath, samples, options("port").toInt)
        case "async" =>
          val someWorker = options.get("worker-ip:worker-port")
            .map(_.split(":").toList match {
              case ip :: port :: Nil => RemoteWorker(ip, port.toInt)
            })
          AsyncWorkerMode(dataPath, samples, options("port").toInt, options("interval").toInt, someWorker)
      }
    }.getOrElse(DefaultMode(options))
  }
}