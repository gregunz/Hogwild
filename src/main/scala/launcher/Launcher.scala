package launcher

import grpc.async.{Worker => AsyncWorker}
import grpc.sync.{Coordinator => SyncCoordinator, Worker => SyncWorker}

object Launcher {

  def main(args: Array[String]): Unit = {
    val arguments = args.toList

    arguments match {
      case "sync" :: "coord" :: tail =>
        SyncCoordinator.run(tail)

      case "sync" :: "worker" :: tail =>
        SyncWorker.run(tail)

      case "async" :: tail =>
        AsyncWorker.run(tail)

      case _ =>
        println(s"arguments invalid: $arguments")
        sys.exit(1)
    }
  }


}
