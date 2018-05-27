package launcher

import dataset.Dataset
import grpc.async.{RemoteWorker, Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}
import launcher.ArgsHandler.Options
import model.StoppingCriteria
import utils.{Interval, IterationsInterval, SecondsInterval, Utils}
import utils.Types.LearningRate

import scala.util.Try

trait Mode {
  def run(): Unit

  def printMode(mode: Mode): Unit = println(s">> Starting $mode")
}

trait TopMode extends Mode {
  val dataset: Dataset
  val lambda: Double
  val stepSize: LearningRate
  def isMaster: Boolean
  def isSlave: Boolean = !isMaster
}

case class SyncWorkerMode(dataset: Dataset, lambda: Double, stepSize: LearningRate, serverIp: String, serverPort: Int)
  extends TopMode {
  def isMaster = false

  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(dataset: Dataset, lambda: Double, stepSize: LearningRate, port: Int,
                               stoppingCriteria: StoppingCriteria) extends TopMode {

  def isMaster = true

  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(dataset: Dataset, lambda: Double, stepSize: LearningRate, port: Int, workerIp: String,
                           workerPort: Int, stoppingCriteria: Option[StoppingCriteria], broadcastInterval: Interval)
  extends TopMode {

  def isMaster: Boolean = stoppingCriteria.isDefined

  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }
}

case class DefaultMode(options: Options, t: Throwable) extends Mode {
  def run(): Unit = println(s"arguments mismatch ($options)\n${t.getMessage}")
}


case class ModeBuilder(dataset: Dataset, lambda: Double, stepSize: LearningRate) {
  def build(serverIp: String, serverPort: Int) =
    SyncWorkerMode(dataset = dataset, lambda = lambda, stepSize = stepSize, serverIp = serverIp,
      serverPort = serverPort)

  def build(port: Int, stoppingCriteria: StoppingCriteria) =
    SyncCoordinatorMode(dataset = dataset, lambda = lambda, stepSize = stepSize, port = port,
      stoppingCriteria = stoppingCriteria)

  def build(port: Int, workerIp: String, workerPort: Int, stoppingCriteria: Option[StoppingCriteria],
            broadcastInterval: Interval) =
    AsyncWorkerMode(dataset = dataset, lambda = lambda, stepSize = stepSize, broadcastInterval = broadcastInterval,
      port = port, stoppingCriteria = stoppingCriteria, workerIp = workerIp, workerPort = workerPort)
}

object Mode {


  def apply(options: Options): Mode = {



    val mode = Try {
      val dataset = Dataset(options("data-path"))
      val modeBuilder = ModeBuilder(dataset = dataset, lambda = options("lambda").toDouble,
        stepSize = options("step-size").toDouble)

      def getInterval(name: String, unit: String): Interval = {
        val limit = options(name).toInt
        options(unit).toLowerCase match {
          case "s" | "sec" | "second" | "seconds" => SecondsInterval(limit)
          case "i" | "it" | "iteration" | "iterations" => IterationsInterval(limit)
        }
      }
      def getStoppingCriteria: StoppingCriteria = {
        StoppingCriteria(dataset, options("early-stopping").toInt, options("min-loss").toDouble,
          getInterval("loss-interval", "loss-interval-unit"))
      }

      options("mode") match {
        case "sync" if options.contains("ip:port") =>
          val (ip, port) = Utils.split("ip:port", ':')
          modeBuilder.build(ip, port.toInt)

        case "sync" if options.contains("port") =>
          modeBuilder.build(options("port").toInt, getStoppingCriteria)
        case "async" =>
          val broadcastInterval = getInterval("broadcast-interval", "broadcast-interval-unit")
          val stoppingCriteria = {
            if (options.contains("early-stopping")) {
              Some(getStoppingCriteria)
            } else {
              None
            }
          }
          val (workerIp, workerPort) = Utils.split(options("ip:port"), ':')
          modeBuilder.build(options("port").toInt, workerIp, workerPort.toInt, stoppingCriteria, broadcastInterval)
      }
    }
    if (mode.isSuccess) {
      mode.get
    } else {
      DefaultMode(options, mode.failed.get)
    }
  }
}
