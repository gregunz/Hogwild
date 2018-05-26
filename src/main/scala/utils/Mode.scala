package utils

import grpc.async.BroadcastersHandler.RemoteWorker
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
  val lambda: Double
  val stepSize: LearningRate
  val interval: Interval
}

case class SyncWorkerMode(dataPath: String, lambda: Double, stepSize: LearningRate,
                          interval: Interval, serverIp: String, serverPort: Int) extends TopMode {
  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(dataPath: String, lambda: Double, stepSize: LearningRate,
                               interval: Interval, port: Int) extends TopMode {
  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(dataPath: String, lambda: Double, stepSize: LearningRate,
                           interval: Interval, port: Int, worker: Option[RemoteWorker]) extends TopMode {
  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }

  val isMaster: Boolean = worker.isDefined
}

case class DefaultMode(options: Options) extends Mode {
  def run(): Unit = println(s"arguments mismatch ($options)")
}


case class ModeBuilder(dataPath: String, lambda: Double, stepSize: LearningRate, interval: Interval){
  def toSyncWorkerMode(serverIp: String, serverPort: Int) = SyncWorkerMode(dataPath = dataPath, lambda = lambda,
    stepSize = stepSize, interval = interval, serverIp = serverIp, serverPort = serverPort)

  def toSyncCoordinatorMode(port: Int) = SyncCoordinatorMode(dataPath = dataPath, lambda = lambda,
    stepSize = stepSize, interval = interval, port = port)

  def toAsyncWorkerMode(port: Int, worker: Option[RemoteWorker]) = AsyncWorkerMode(
    dataPath = dataPath, lambda = lambda, stepSize = stepSize, interval = interval, port = port, worker = worker)
}

object Mode {
  def apply(options: Options): Mode = {
    Try {
      val modeBuilder = ModeBuilder(dataPath = options("data-path"), lambda = options("lambda").toDouble,
        stepSize = options("step-size").toDouble, interval = Interval(options("interval").toInt,
          inSecond = options("in-second").toBoolean))

      options("mode") match {
        case "sync" if options.contains("ip:port") =>
          options("ip:port").split(":").toList match {
            case ip :: port :: Nil => modeBuilder.toSyncWorkerMode(ip, port.toInt)
          }
        case "sync" if options.contains("port") =>
          modeBuilder.toSyncCoordinatorMode(options("port").toInt)
        case "async" =>
          val someWorker = options.get("ip:port")
            .map(_.split(":").toList match {
              case ip :: port :: Nil => RemoteWorker(ip, port.toInt)
            })
          modeBuilder.toAsyncWorkerMode(options("port").toInt, someWorker)
      }
    }.getOrElse(DefaultMode(options))
  }
}
