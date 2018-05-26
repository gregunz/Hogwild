package launcher

import grpc.async.BroadcastersHandler.RemoteWorker
import grpc.async.{Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}
import launcher.ArgsHandler.Options
import utils.Interval
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
  val isMaster: Boolean
}

case class SyncWorkerMode(dataPath: String, lambda: Double, stepSize: LearningRate, interval: Interval,
                          serverIp: String, serverPort: Int) extends TopMode {
  val isMaster = false

  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(dataPath: String, lambda: Double, stepSize: LearningRate,
                               interval: Interval, port: Int, maxTimesWithoutImproving: Int) extends TopMode {
  val isMaster = true

  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(dataPath: String, lambda: Double, stepSize: LearningRate, interval: Interval, port: Int,
                           worker: Option[RemoteWorker], maxTimesWithoutImproving: Option[Int]) extends TopMode {
  require(
    (worker.isEmpty && maxTimesWithoutImproving.isDefined)
      || (worker.isDefined && maxTimesWithoutImproving.isEmpty)
  )

  val isMaster: Boolean = maxTimesWithoutImproving.isDefined

  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }
}

case class DefaultMode(options: Options, t: Throwable) extends Mode {
  def run(): Unit = println(s"arguments mismatch ($options)\n${t.getMessage}")
}


case class ModeBuilder(dataPath: String, lambda: Double, stepSize: LearningRate, interval: Interval) {
  def toSyncWorkerMode(serverIp: String, serverPort: Int) =
    SyncWorkerMode(dataPath = dataPath, lambda = lambda, stepSize = stepSize, interval = interval, serverIp = serverIp,
      serverPort = serverPort)

  def toSyncCoordinatorMode(port: Int, maxTimesWithoutImproving: Int) =
    SyncCoordinatorMode(dataPath = dataPath, lambda = lambda, stepSize = stepSize, interval = interval, port = port,
      maxTimesWithoutImproving = maxTimesWithoutImproving)

  def toAsyncWorkerMode(port: Int, worker: Option[RemoteWorker], maxTimesWithoutImproving: Option[Int]) =
    AsyncWorkerMode(dataPath = dataPath, lambda = lambda, stepSize = stepSize, interval = interval, port = port,
      worker = worker, maxTimesWithoutImproving = maxTimesWithoutImproving)
}

object Mode {
  def apply(options: Options): Mode = {
    val mode = Try {
      val modeBuilder = ModeBuilder(dataPath = options("data-path"), lambda = options("lambda").toDouble,
        stepSize = options("step-size").toDouble, interval = Interval(options("interval").toInt,
          inSecond = options("in-second") == "1"))

      options("mode") match {
        case "sync" if options.contains("ip:port") =>
          options("ip:port").split(":").toList match {
            case ip :: port :: Nil => modeBuilder.toSyncWorkerMode(ip, port.toInt)
          }
        case "sync" if options.contains("port") =>
          modeBuilder.toSyncCoordinatorMode(options("port").toInt, options("early-stopping").toInt)
        case "async" =>
          val someWorker = options.get("ip:port")
            .map(_.split(":").toList match {
              case ip :: port :: Nil => RemoteWorker(ip, port.toInt)
            })
          val earlyStopping = if (someWorker.isEmpty) {
            Some(options("early-stopping").toInt)
          } else {
            None
          }
          modeBuilder.toAsyncWorkerMode(options("port").toInt, someWorker, earlyStopping)
      }
    }
    if (mode.isSuccess) {
      mode.get
    } else {
      DefaultMode(options, mode.failed.get)
    }
  }
}
