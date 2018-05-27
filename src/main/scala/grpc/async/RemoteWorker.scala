package grpc.async

case class RemoteWorker(ip: String, port: Int, name: Option[String]){

  val uid: String = s"$ip:$port"

  override def toString: String = name.map(_ + "@").getOrElse("") + uid

  def toWorkerDetail: WorkerDetail = WorkerDetail(ip = ip, port = port, name = name.getOrElse(""))
}

object RemoteWorker {
  def fromWorkerDetails(w: WorkerDetail): RemoteWorker = {
    RemoteWorker( ip = w.ip, port = w.port, name = Some(w.name).filter(_ != ""))
  }
}