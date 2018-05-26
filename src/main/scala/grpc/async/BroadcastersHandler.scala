package grpc.async

import dataset.Dataset
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import model.SparseNumVector
import utils.Types.TID


case class BroadcastersHandler(dataset: Dataset, myId: Int) {

  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  type Broadcaster = (ManagedChannel, StreamObserver[BroadcastMessage])
  private val instance = this
  private var broadcasters: Map[RemoteWorker, Broadcaster] = Map.empty
  private var waitingList: Set[RemoteWorker] = Set.empty
  private var tidsPerBroadcaster: Map[Int, Set[TID]] = Map.empty

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

  def updateTidsPerBroadcaster(): Unit = {
    val tids = dataset.tids
    val nGroup = this.broadcasters.size
    val groupSize = Math.round(tids.size / nGroup.toDouble)
    tidsPerBroadcaster = this.broadcasters.keys.map(_.id).toList.map()
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

  def broadcast(weights: SparseNumVector[Double]): Unit = {
    instance.synchronized {
      if (broadcasters.nonEmpty) {
        println(s"[SEND] feel like sharing some computations, here you go guys " +
          s"${broadcasters.keySet.mkString("[", ";", "]")}")
      }
      broadcasters.foreach{ case (worker, (_, broadcaster)) =>
        val tidsToBroadcast = tidsPerBroadcaster(worker.id) ++ tidsToBroadcast(myId)
        val msg = BroadcastMessage(
          weightsUpdate = weights.filterKeys().toMap,
          workerDetail = Some(worker.toWorkerDetail)
        )
          broadcaster.onNext(msg)
      }
    }
  }

  def activeWorkers: Set[RemoteWorker] = instance.synchronized {
    broadcasters.keySet ++ waitingList
  }

}
