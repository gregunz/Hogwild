package grpc.async

case class RemoteWorker(ip: String, port: Int, id: Int){
  def toWorkerDetail: WorkerDetail = WorkerDetail(address = ip, port = port)
}