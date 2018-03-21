package demo.communication

import demo.communication.DistributeTaskGrpc.{DistributeTaskBlockingStub, DistributeTaskStub}
import io.grpc.ManagedChannelBuilder

object Client extends App {

  val channel = ManagedChannelBuilder
    .forAddress("localhost", 50051) // host and port of service
    .usePlaintext(true) // don't use encryption (for demo purposes)
    .build

  // Send a first task with dimension -1 so that the server knows we are a new node
  var task = Task(taskNumber = 1, dimention = -1, value = 1.0f)

  val blockingStub : DistributeTaskBlockingStub = DistributeTaskGrpc.blockingStub(channel)

  var stillWorkToDo = true
  // Iterate until no more work to do from server (server sends -1 in dimension param)
  while(stillWorkToDo) {
    task = blockingStub.askForTask(task)
    println(task.value)
    if (task.dimention == -1) {
      stillWorkToDo = false
    }
  }
  println("Bye")
// To do: send back gradient once we have received every weight from server.
}
