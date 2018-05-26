package grpc.async

case class RemoteWorker(ip: String, port: Int, id: Int){
  def toWorkerDetail: WorkerDetail = WorkerDetail(ip = ip, port = port)
}

object RemoteWorker {
  def fromWorkerDetails(w: WorkerDetail): RemoteWorker = {
    RemoteWorker( ip = w.ip, port = w.port, id = w.id)
  }
}