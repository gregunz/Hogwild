package grpc.async

import grpc.async.Worker.{Broadcaster, RemoteWorker, Stub}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.grpc.stub.StreamObserver

object BroadcastersHandler {
  private val instance = this
  private var broadcasters: Map[RemoteWorker, Broadcaster] = Map.empty

  def add(worker: RemoteWorker, channel: ManagedChannel): Unit = {
    if (!this.workers.contains(worker)){
      add(createBroadcaster(worker, createStub(channel)))
    }
  }

  def add(worker: RemoteWorker): Unit = {
    if (!this.workers.contains(worker)){
      add(createBroadcaster(worker))
    }
  }

  def add(workers: Set[RemoteWorker]): Unit = {
    instance.synchronized{
      broadcasters ++= workers.diff(this.workers).map(createBroadcaster).toMap
    }
  }

  def remove(worker: RemoteWorker): Unit = {
    instance.synchronized {
      broadcasters = broadcasters.filterKeys(w => w != worker)
      println(s"[BYE] Goodbye my lover, goodbye my friend... ($worker left)")
    }
  }

  def broadcast(msg: BroadcastMessage): Unit = {
    broadcasters.values.foreach(_.onNext(msg))
  }

  def workers: Set[RemoteWorker] = instance.synchronized{broadcasters.keySet}

  def hasBroadcaster: Boolean = instance.synchronized{broadcasters.nonEmpty}

  private def add(e: (RemoteWorker, Broadcaster)): Map[RemoteWorker, Broadcaster] = {
    instance.synchronized {
      broadcasters += e
      println(s"[NEW]: a new worker just arrived, welcome to the GANG mate (${e._1} joined)")
      broadcasters
    }
  }

  private def createBroadcaster(worker: RemoteWorker, stub: Stub): (RemoteWorker, Broadcaster) = {
    val broadcastObserver = new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = {
        print(t.printStackTrace())
        instance.remove(worker)
      }

      override def onCompleted(): Unit = instance.remove(worker)

      override def onNext(msg: Empty): Unit = {}
    }
    worker -> stub.broadcast(broadcastObserver)
  }

  private def createBroadcaster(worker: RemoteWorker): (RemoteWorker, Broadcaster) = {
    createBroadcaster(worker, createStub(worker))
  }

  private def createStub(channel: ManagedChannel): Stub = WorkerServiceAsyncGrpc.stub(channel)

  private def createStub(worker: RemoteWorker): Stub = {
    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build

    createStub(channel)
  }

}
