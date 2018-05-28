package launcher.mode

import dataset.Dataset
import launcher.ArgsHandler.Options
import model.StoppingCriteria
import utils._

import scala.util.Try

trait Mode {
  def run(): Unit

  def printMode(mode: Mode): String = s"Starting $mode"
}


object Mode {


  def apply(options: Options): Mode = {

    val mode = Try {
      val logger = Logger(options("log").toInt)
      val dataset = Dataset(logger, options("data-path"))
      val seed = options("seed").toLong
      val modeBuilder = ModeBuilder(seed = seed, logger = logger, name = options.get("name"), dataset = dataset,
        lambda = options("lambda").toDouble, stepSize = options("step-size").toDouble)

      def getInterval(name: String, unit: String): Interval = {
        val limit = options(name).toInt
        options(unit).toLowerCase match {
          case "s" | "sec" | "second" | "seconds" => SecondsInterval(limit)
          case "i" | "it" | "iteration" | "iterations" => IterationsInterval(limit)
        }
      }

      def getStoppingCriteria: StoppingCriteria = {
        StoppingCriteria(logger, dataset, options("early-stopping").toInt, options("min-loss").toDouble,
          getInterval("loss-interval", "loss-interval-unit"))
      }

      options("mode") match {
        case "sync" if options.contains("early-stopping") =>
          modeBuilder.build(options("port").toInt, getStoppingCriteria)

        case "sync" =>
          val (ip, port) = Utils.split(options("ip:port"), ':')
          modeBuilder.build(ip, port.toInt)

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
          modeBuilder.build(options("port").toInt, workerIp, workerPort.toInt, stoppingCriteria,
            broadcastInterval)
      }
    }
    if (mode.isSuccess) {
      mode.get
    } else {
      DefaultMode(options, mode.failed.get)
    }
  }
}
