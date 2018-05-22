package grpc.async

import grpc.async.Worker.{Broadcaster, Stub, RemoteWorker}
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

object BroadcastersHandler {
  private val instance = this
  private var broadcasters: Map[RemoteWorker, Broadcaster] = Map.empty

  def add(worker: RemoteWorker): Map[RemoteWorker, Broadcaster] = {
    if(!this.workers.contains(worker)){
      add(Set(worker))
      println(s"[NEW]: a new worker just arrived, welcome to the GANG mate ($worker joined)")
    }
    broadcasters
  }
  def add(workers: Set[RemoteWorker]): Map[RemoteWorker, Broadcaster] = {
    instance.synchronized{
      broadcasters ++= workers.diff(this.workers).map(w => w -> createBroadcaster(w)).toMap
      broadcasters
    }
  }

  def remove(worker: RemoteWorker): Map[RemoteWorker, Broadcaster] = {
    instance.synchronized {
      broadcasters = broadcasters.filterKeys(w => w != worker)
      println(s"[BYE] Goodbye my lover, goodbye my friend... ($worker left)")
      broadcasters
    }
  }

  def broadcast(msg: BroadcastMessage): Unit = {
    broadcasters.values.foreach(_.onNext(msg))
  }

  def workers: Set[RemoteWorker] = instance.synchronized{broadcasters.keySet}

  def hasBroadcaster: Boolean = instance.synchronized{broadcasters.nonEmpty}

  private def createBroadcaster(worker: RemoteWorker): Broadcaster = {
    val broadcastObserver = new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = instance.remove(worker)

      override def onCompleted(): Unit = instance.remove(worker)

      override def onNext(msg: Empty): Unit = {}
    }
    createStub(worker).broadcast(broadcastObserver)
  }

  private def createStub(worker: RemoteWorker): Stub = {
    val channel = ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build

    WorkerServiceAsyncGrpc.stub(channel)
  }

}
