package grpc.async

import grpc.async.Worker.{Broadcaster, RemoteWorker, Stub}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

object BroadcastersHandler {
  private val instance = this
  private var broadcasters: Map[RemoteWorker, Broadcaster] = Map.empty
  private var waitingList: Set[RemoteWorker] = Set.empty

  def add(worker: RemoteWorker): Unit = {
    instance.synchronized {
      if (!broadcasters.contains(worker)) {
        waitingList -= worker
        println(s"[NEW] a new worker just joined the gang! welcome $worker")
        broadcasters += createBroadcaster(worker)
      }
    }
  }

  def addSomeActive(workers: Set[RemoteWorker]): Unit = {
    instance.synchronized {
      val newWorkers = workers.diff(broadcasters.keySet)
      if (newWorkers.nonEmpty) {
        waitingList --= newWorkers
        broadcasters ++= newWorkers.map(createBroadcaster)
      }
    }
  }

  private def createBroadcaster(worker: RemoteWorker): (RemoteWorker, Broadcaster) = {
    createBroadcaster(worker, createStub(worker))
  }

  private def createBroadcaster(worker: RemoteWorker, stub: Stub): (RemoteWorker, Broadcaster) = {
    val broadcastObserver = new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = {
        //t.printStackTrace()
        instance.remove(worker)
      }

      override def onCompleted(): Unit = instance.remove(worker)

      override def onNext(msg: Empty): Unit = {}
    }
    worker -> stub.broadcast(broadcastObserver)
  }

  def remove(worker: RemoteWorker): Unit = {
    instance.synchronized {
      if (broadcasters.contains(worker)) {
        broadcasters = broadcasters.filterKeys(w => w != worker)
        println(s"[BYE] Goodbye my lover, goodbye my friend... ($worker left)")
      }
    }
  }

  private def createStub(worker: RemoteWorker): Stub = {
    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build

    createStub(channel)
  }

  private def createStub(channel: ManagedChannel): Stub = WorkerServiceAsyncGrpc.stub(channel)

  def addToWaitingList(worker: RemoteWorker): Unit = {
    instance.synchronized {
      waitingList += worker
    }
  }

  def broadcast(msg: BroadcastMessage): Unit = {
    instance.synchronized {
      if (broadcasters.nonEmpty) {
        println(s"[SEND] feel like sharing some computations, here you go guys " +
          s"(${broadcasters.keySet.mkString("[", ";", "]")})")
      }
      broadcasters.values.foreach(_.onNext(msg))
    }
  }

  def activeWorkers: Set[RemoteWorker] = instance.synchronized {
    broadcasters.keySet ++ waitingList
  }

}
