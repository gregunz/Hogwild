package demo.communication

import scala.concurrent.{ExecutionContext, Future}

object Server extends GrpcServer {

  def main(args: Array[String]): Unit = {
    val ssd = DistributeTaskGrpc.bindService(DistributeTaskService, ExecutionContext.global)
    runServer(ssd)
  }

  object DistributeTaskService extends DistributeTaskGrpc.DistributeTask {

    override def askForTask(request: Task): Future[Task] = {
      // Receive request for new task, we get -1 in dimension if new node or previous dimension that we sent
      println(request.value)
      val newTask = request.dimention match {
        case -1 => Task(taskNumber = 10, dimention = 10, value = 10.0f) // new client
        case 30 => Task(taskNumber = 10, dimention = -1, value = 10.0f) // existing client, more work to do
        case _  => Task(taskNumber = request.dimention, dimention = request.dimention + 1, value = request.value/2f + 1f)
          // existing client, no more work to do
      }

      Future.successful(newTask)
    }
    // To Do !!!
    override def returnResult(request: Task): Future[Task] = {

      println(request.value)

      val newTask = request.dimention match {
        case 20 => Task(taskNumber = 10, dimention = -1, value = 10.0f)
        case _  => Task(taskNumber = request.dimention, dimention = request.dimention + 1, value = 0.1f)
      }
      Future.successful(newTask)
    }
  }
}
