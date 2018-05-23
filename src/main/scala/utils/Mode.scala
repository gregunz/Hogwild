package utils

import grpc.async.Worker.RemoteWorker
import grpc.async.{Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}
import launcher.ArgsHandler.Options
import utils.Types.LearningRate

import scala.util.Try

trait Mode {
  def run(): Unit
  def printMode(mode: Mode): Unit = println(s">> Starting $mode")
}

trait TopMode extends Mode {
  val dataPath: String
  val samples: Boolean
  val lambda: Double
  val stepSize: LearningRate
  val interval: Int
}

case class SyncWorkerMode(dataPath: String, samples: Boolean, lambda: Double, stepSize: LearningRate,
                          interval: Int, serverIp: String, serverPort: Int) extends TopMode {
  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(dataPath: String, samples: Boolean, lambda: Double, stepSize: LearningRate,
                               interval: Int, port: Int) extends TopMode {
  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(dataPath: String, samples: Boolean, lambda: Double, stepSize: LearningRate,
                           interval: Int, port: Int, worker: Option[RemoteWorker]) extends TopMode {
  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }
}

case class DefaultMode(options: Options) extends Mode {
  def run(): Unit = println(s"arguments mismatch ($options)")
}


case class ModeBuilder(dataPath: String, samples: Boolean, lambda: Double, stepSize: LearningRate, interval: Int){
  def toSyncWorkerMode(serverIp: String, serverPort: Int) = SyncWorkerMode(dataPath = dataPath, samples = samples,
    lambda = lambda, stepSize = stepSize, interval = interval, serverIp = serverIp, serverPort = serverPort)

  def toSyncCoordinatorMode(port: Int) = SyncCoordinatorMode(dataPath = dataPath, samples = samples,
    lambda = lambda, stepSize = stepSize, interval = interval, port = port)

  def toAsyncWorkerMode(port: Int, worker: Option[RemoteWorker]) = AsyncWorkerMode(dataPath = dataPath,
    samples = samples, lambda = lambda, stepSize = stepSize, interval = interval, port = port, worker = worker)
}

object Mode {
  def apply(options: Options): Mode = {
    Try {
      val modeBuilder = ModeBuilder(dataPath = options("data-path"), samples = options("samples").toInt == 1,
        lambda = options("lambda").toDouble, stepSize = options("step-size").toDouble, interval = options("interval").toInt)

      options("mode") match {
        case "sync" if options.contains("server-ip:server-port") =>
          options("server-ip:server-port").split(":").toList match {
            case ip :: port :: Nil => modeBuilder.toSyncWorkerMode(ip, port.toInt)
          }
        case "sync" if options.contains("port") =>
          modeBuilder.toSyncCoordinatorMode(options("port").toInt)
        case "async" =>
          val someWorker = options.get("worker-ip:worker-port")
            .map(_.split(":").toList match {
              case ip :: port :: Nil => RemoteWorker(ip, port.toInt)
            })
          modeBuilder.toAsyncWorkerMode(options("port").toInt, someWorker)
      }
    }.getOrElse(DefaultMode(options))
  }
}
