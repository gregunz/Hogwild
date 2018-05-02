package  grpc.async

import dataset.Dataset
import grpc.sync.WorkerServiceGrpc.WorkerServiceStub
import grpc.sync.{WorkerBroadcast, WorkerServiceGrpc}
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import model.{GrpcServer, SVM, SlavesHandler, SparseNumVector}
import utils.Label.Label
import utils.Types.TID

import scala.collection.immutable
import scala.concurrent.ExecutionContext

object Worker extends GrpcServer {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val instance = this
  var count = 0
  val svm = SVM()

  def main(args: Array[String]): Unit = {
    println("Please enter a worker ID")
    val WorkerId = readInt()
    println(WorkerId)
    load()

    establishCommunications(WorkerId)
  }

  def load(): Unit = {
    Dataset.load()
    samples
  }

  def establishCommunications(WorkerID : Int): Unit = {

    val portNumber = 50050 + WorkerID
    val ssd = WorkerServiceGrpc.bindService(WorkerService, ExecutionContext.global)

    var activeWorker: Boolean = true

    if (WorkerID > 1) {

      val portNumbers = List(50050, 50051)

      val channels = portNumbers.map( portNumber =>
        ManagedChannelBuilder
          .forAddress("localhost", portNumber)
          .usePlaintext(true)
          .build
      )

      val clients: List[WorkerServiceStub] = channels.map(WorkerServiceGrpc.stub(_))


      val channel = ManagedChannelBuilder
        .forAddress("localhost", portNumber)
        .usePlaintext(true)
        .build
      val client: WorkerServiceStub = WorkerServiceGrpc.stub(channel)

      val HelloMessage = new StreamObserver[WorkerBroadcast] {
        override def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          sys.exit(1)
        }

        override def onCompleted(): Unit = {
          println("ON_COMPLETED")
          channel.shutdown()
          sys.exit(0)
        }

        override def onNext(message: WorkerBroadcast): Unit = {
          if (message.counter > 30) {
            println(s"All messages have been received, Terminating")
            this.onCompleted()
            channel.shutdown
          }
        }
      }

      val messageObservers = clients.map( client => client.updateWeights(HelloMessage) )

      while (activeWorker) {

        println(count)
        messageObservers.foreach(observer => observer.onNext(WorkerBroadcast(WorkerID, "Test", count)))

      }

      /*
      while (!channel.isShutdown) {
        if (count > 300) {
          println(s"All messages have been received, Terminating")
          channel.shutdown()
        } else {
          //count += 1
          println(count)
          messageObserver.onNext(WorkerBroadcast(WorkerID, "Test", count))
        }
      }
      */

      println(s"Channel on port $portNumber has been shut down")
      sys.exit(0)

    }

    println(">> SERVER READY <<")
    runServer(ssd, portNumber)
  }

  object WorkerService extends WorkerServiceGrpc.WorkerService {

    val instance = this

    override def updateWeights(responseObserver: StreamObserver[WorkerBroadcast]): StreamObserver[WorkerBroadcast] =
      new StreamObserver[WorkerBroadcast] {
        override def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
        }

        override def onCompleted(): Unit = {
          println("ON_COMPLETED")
        }

        override def onNext(message: WorkerBroadcast): Unit = {
          val workerId = message.myId
          val content = message.msg
          val counter = message.counter
          count += 1
          println(s"[MSG $counter] Worker $workerId says : $content")
          responseObserver.onNext(WorkerBroadcast(myId = 0, msg = "My_updates", counter = count))

        }
      }
  }
}