package launcher

import dataset.Dataset
import grpc.async.{RemoteWorker, Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}
import launcher.ArgsHandler.Options
import model.StoppingCriteria
import utils.Interval
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
  val interval: Interval
  val isMaster: Boolean
}

case class SyncWorkerMode(dataset: Dataset, lambda: Double, stepSize: LearningRate, interval: Interval,
                          serverIp: String, serverPort: Int) extends TopMode {
  val isMaster = false

  def run(): Unit = {
    printMode(this)
    SyncWorker.run(this)
  }
}

case class SyncCoordinatorMode(dataset: Dataset, lambda: Double, stepSize: LearningRate, interval: Interval, port: Int,
                               stoppingCriteria: StoppingCriteria) extends TopMode {
  val isMaster = true

  def run(): Unit = {
    printMode(this)
    SyncCoordinator.run(this)
  }
}

case class AsyncWorkerMode(dataset: Dataset, lambda: Double, stepSize: LearningRate, interval: Interval, port: Int,
                           moreArgs: AsyncMoreArgs) extends TopMode {

  val isMaster: Boolean = moreArgs match {
    case MasterArgs(_) => true
    case SlaveArgs(_, _) => false
  }

  def run(): Unit = {
    printMode(this)
    AsyncWorker.run(this)
  }
}

trait AsyncMoreArgs {}
case class MasterArgs(stoppingCriteria: StoppingCriteria) extends AsyncMoreArgs
case class SlaveArgs(masterIp: String, masterPort: String) extends AsyncMoreArgs

case class DefaultMode(options: Options, t: Throwable) extends Mode {
  def run(): Unit = println(s"arguments mismatch ($options)\n${t.getMessage}")
}


case class ModeBuilder(dataset: Dataset, lambda: Double, stepSize: LearningRate, interval: Interval) {
  def build(serverIp: String, serverPort: Int) =
    SyncWorkerMode(dataset = dataset, lambda = lambda, stepSize = stepSize, interval = interval, serverIp = serverIp,
      serverPort = serverPort)

  def build(port: Int, stoppingCriteria: StoppingCriteria) =
    SyncCoordinatorMode(dataset = dataset, lambda = lambda, stepSize = stepSize, interval = interval, port = port,
      stoppingCriteria = stoppingCriteria)

  def build(port: Int, moreArgs: AsyncMoreArgs) =
    AsyncWorkerMode(dataset = dataset, lambda = lambda, stepSize = stepSize, interval = interval, port = port,
      moreArgs = moreArgs)
}

object Mode {
  def apply(options: Options): Mode = {
    val mode = Try {
      val dataset = Dataset(options("data-path"))
      val modeBuilder = ModeBuilder(dataset = dataset, lambda = options("lambda").toDouble,
        stepSize = options("step-size").toDouble, interval = Interval(options("interval").toInt,
          inSecond = options("in-second") == "1"))

      options("mode") match {
        case "sync" if options.contains("ip:port") =>
          options("ip:port").split(":").toList match {
            case ip :: port :: Nil => modeBuilder.build(ip, port.toInt)
          }
        case "sync" if options.contains("port") =>
          val stoppingCriteria = StoppingCriteria(dataset, options("early-stopping").toInt, options("min-loss").toDouble)
          modeBuilder.build(options("port").toInt, stoppingCriteria)
        case "async" =>
          val moreArgs = {
            if (options("master") == "1") {
              val stoppingCriteria = StoppingCriteria(dataset, options("early-stopping").toInt, options("min-loss").toDouble)
              MasterArgs(stoppingCriteria)
            } else {
              options("ip:port").split(":").toList match {
                  case ip :: port :: Nil => SlaveArgs(ip, port)
              }
            }
          }
          modeBuilder.build(options("port").toInt, moreArgs)
      }
    }
    if (mode.isSuccess) {
      mode.get
    } else {
      DefaultMode(options, mode.failed.get)
    }
  }
}
