package grpc.async

import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}


object BroadcastersHandler {

  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  type Broadcaster = (ManagedChannel, StreamObserver[BroadcastMessage])
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

  private def createBroadcaster(worker: RemoteWorker): (RemoteWorker, Broadcaster) = {
    createBroadcaster(worker, createChannel(worker))
  }

  private def createBroadcaster(worker: RemoteWorker, channel: ManagedChannel): (RemoteWorker, Broadcaster) = {
    val broadcastObserver = new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = {
        instance.remove(worker)
      }

      override def onCompleted(): Unit = instance.remove(worker)

      override def onNext(msg: Empty): Unit = {}
    }

    val stub = createStub(channel)
    worker -> (channel -> stub.broadcast(broadcastObserver))
  }

  def remove(worker: RemoteWorker): Unit = {
    instance.synchronized {
      if (broadcasters.contains(worker)) {
        broadcasters = broadcasters.filterKeys(w => w != worker)
        println(s"[BYE] Goodbye my lover, goodbye my friend... ($worker left)")
      }
    }
  }

  private def createStub(channel: ManagedChannel): Stub = WorkerServiceAsyncGrpc.stub(channel)

  private def createChannel(worker: RemoteWorker): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build
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

  def killAll(): Unit = {
    instance.synchronized {
      val channels = broadcasters.values.unzip._1 ++ waitingList.map(createChannel)
      channels.foreach { c =>
        val blockingStub = WorkerServiceAsyncGrpc.blockingStub(c)
        blockingStub.kill(Empty())
      }
    }
  }

  def addToWaitingList(worker: RemoteWorker): Unit = {
    instance.synchronized {
      waitingList += worker
    }
  }

  def broadcast(msg: BroadcastMessage): Unit = {
    instance.synchronized {
      if (broadcasters.nonEmpty) {
        println(s"[SEND] feel like sharing some computations, here you go guys " +
          s"${broadcasters.keySet.mkString("[", ";", "]")}")
      }
      broadcasters.values.foreach(_._2.onNext(msg))
    }
  }

  def activeWorkers: Set[RemoteWorker] = instance.synchronized {
    broadcasters.keySet ++ waitingList
  }

  case class RemoteWorker(ip: String, port: Int)

}
