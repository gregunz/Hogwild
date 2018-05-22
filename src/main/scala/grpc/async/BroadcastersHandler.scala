package grpc.async

import grpc.async.Worker.{Broadcaster, Stub, RemoteWorker}
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

object BroadcastersHandler {
  private val handler = this
  private var broadcasters: Map[RemoteWorker, Broadcaster] = Map.empty

  def add(worker: RemoteWorker): Map[RemoteWorker, Broadcaster] = {
    if(!this.workers.contains(worker)){
      println(s"[NEW]: a new worker just arrived, welcome to the GANG mate ($worker joined)")
    }
    add(Set(worker))
  }
  def add(workers: Set[RemoteWorker]): Map[RemoteWorker, Broadcaster] = {
    broadcasters ++= workers.diff(this.workers).map(w => w -> createBroadcaster(w)).toMap
    broadcasters
  }

  def remove(worker: RemoteWorker): Map[RemoteWorker, Broadcaster] = {
    println(s"[BYE] Goodbye my lover, goodbye my friend... ($worker left)")
    broadcasters = broadcasters.filterKeys(w => w != worker)
    broadcasters
  }

  def broadcast(msg: BroadcastMessage): Unit = {
    broadcasters.values.foreach(_.onNext(msg))
  }

  def workers: Set[RemoteWorker] = broadcasters.keySet

  def hasBroadcaster: Boolean = broadcasters.nonEmpty

  private def createBroadcaster(worker: RemoteWorker): Broadcaster = {
    val broadcastObserver = new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = handler.remove(worker)

      override def onCompleted(): Unit = handler.remove(worker)

      override def onNext(msg: Empty): Unit = println("<< SHOULD NEVER BE TRIGGERED >>")
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
