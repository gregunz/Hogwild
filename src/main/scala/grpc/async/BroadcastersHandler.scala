package grpc.async

import dataset.Dataset
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import model.SparseNumVector
import utils.Types.TID
import utils.{Interval, Logger}

import scala.util.Try


case class BroadcastersHandler(logger: Logger, dataset: Dataset, meWorker: RemoteWorker, broadcastInterval: Interval) {

  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  type Broadcaster = (ManagedChannel, StreamObserver[BroadcastMessage])
  private val instance = this
  private var broadcasters: Map[RemoteWorker, Broadcaster] = Map.empty
  private var waitingList: Set[RemoteWorker] = Set.empty
  private var tidsPerBroadcaster: Map[String, Set[TID]] = Map.empty

  def add(worker: RemoteWorker): Unit = {
    instance.synchronized {
      if (!broadcasters.contains(worker)) {
        waitingList -= worker
        logger.log(2)(s"[NEW] a new worker just joined the gang! welcome $worker")
        broadcasters += createBroadcaster(worker)
        updateTidsPerBroadcaster()
      }
    }
  }

  def addSomeActive(workers: Set[RemoteWorker]): Unit = {
    instance.synchronized {
      val newWorkers = workers.diff(broadcasters.keySet)
      if (newWorkers.nonEmpty) {
        waitingList --= newWorkers
        broadcasters ++= newWorkers.map(createBroadcaster)
        updateTidsPerBroadcaster()
      }
    }
  }

  private def createBroadcaster(worker: RemoteWorker): (RemoteWorker, Broadcaster) = {
    createBroadcaster(worker, createChannel(worker))
  }

  private def createChannel(worker: RemoteWorker): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build
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
        updateTidsPerBroadcaster()
        logger.log(2)(s"[BYE] Goodbye my lover, goodbye my friend... ($worker left)")
      }
    }
  }

  private def updateTidsPerBroadcaster(): Unit = {

    val uids = (this.broadcasters.keySet + meWorker)
      .map(_.uid)
      .toList
      .sorted

    val tidsGrouped = cut(dataset.tids, uids.size)
      .map(_.toSet)
      .toList

    require(uids.size == tidsGrouped.size, "grouping not done correctly :(")

    val myTids = tidsGrouped(uids.indexOf(meWorker.uid))

    tidsPerBroadcaster = (uids zip tidsGrouped.map(_ ++ myTids)).toMap
  }

  private def cut[A](xs: Seq[A], n: Int) = {
    val (quot, rem) = (xs.size / n, xs.size % n)
    val (smaller, bigger) = xs.splitAt(xs.size - rem * (quot + 1))
    smaller.grouped(quot) ++ bigger.grouped(quot + 1)
  }

  private def createStub(channel: ManagedChannel): Stub = WorkerServiceAsyncGrpc.stub(channel)

  def killAll(): Unit = {
    instance.synchronized {
      val channels = broadcasters.values.unzip._1 ++ waitingList.map(createChannel)
      channels.foreach { c =>
        Try {
          val blockingStub = WorkerServiceAsyncGrpc.blockingStub(c)
          blockingStub.kill(Empty())
        }
      }
    }
  }

  def addToWaitingList(worker: RemoteWorker): Unit = {
    instance.synchronized {
      waitingList += worker
    }
  }

  def broadcast(weightsUpdate: SparseNumVector[Double]): Unit = {
    instance.synchronized {
      if (broadcasters.nonEmpty) {
        logger.log(3)(s"[SEND] feel like sharing some computations, here you go guys " +
          s"${broadcasters.keySet.mkString("[", ", ", "]")}")
      }
      broadcasters.par.foreach { case (worker, (_, broadcaster)) =>
        val msg = BroadcastMessage(
          weightsUpdate = weightsUpdate.filterKeys(tidsPerBroadcaster(worker.uid)).toMap,
          workerDetail = Some(meWorker.toWorkerDetail)
        )
        broadcaster.onNext(msg)
      }
    }
  }

  def allWorkers: Set[RemoteWorker] = instance.synchronized {
    broadcasters.keySet ++ waitingList + meWorker
  }

}
