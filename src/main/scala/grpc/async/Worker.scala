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

  val connectionsHandler = new ConnectionsHandler
  var id_to_send = -1

  val instance = this
  var count = 0

  val lambda: Double = 0.1
  val svm = SVM()

  /* TO COMPUTE & PRINT LOSSES */
  val someDids: Set[TID] = Dataset.didSet.take(500)
  val someFeatures: immutable.IndexedSeq[SparseNumVector] = Dataset.features.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  val someLabels: immutable.IndexedSeq[Label] = Dataset.labels.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  var i = 0
  var time: Long = System.currentTimeMillis()

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
      portNumbers.foreach( connectionsHandler.addWorker(_))

      val clients: List[WorkerServiceStub] = channels.map(WorkerServiceGrpc.stub(_))

      val HelloMessage = new StreamObserver[WorkerBroadcast] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          sys.exit(1)
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          sys.exit(0)
        }

        def onNext(message: WorkerBroadcast): Unit = {
          id_to_send = WorkerID
          //println(id_to_send)
          if (message.counter > 300) {
            println(s"All messages have been received, Terminating")
            this.onCompleted
          }
        }
      }



      val messageObservers = clients.map( client => client.updateWeights(HelloMessage) )




      while (activeWorker) {
        println(id_to_send)
        //println(count)

        messageObservers.foreach(observer => observer.onNext(WorkerBroadcast(id_to_send, "Test", portNumber, count)))

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
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
        }

        def onNext(message: WorkerBroadcast): Unit = {
          if (message.myId != -1) {
            instance.synchronized {
              connectionsHandler.addTest(message.myId)
              if (connectionsHandler.isWaitingOnSomeUpdates) {
                instance.wait()
              } else {
                //svm.updateWeight(connectionsHandler.getMeanGradient)
                if (i % 500 == 0) {

                  val loss = connectionsHandler.getTest
                  val duration = System.currentTimeMillis() - time
                  time = System.currentTimeMillis()
                  println(s"[UPT][$i][$duration]: loss = $loss}")
                }
                i += 1
                instance.notifyAll()
              }
            }
          } else {
            if (!(connectionsHandler.getPorts contains message.port)) {
              println("[NEW]: a new worker has arrived !")
              connectionsHandler.addWorker(message.port)
            }
          }
          val workerId = message.myId
          val content = message.msg
          val port = message.port
          count += 1
          //println(s"[MSG $counter] Worker $workerId says : $content")
          responseObserver.onNext(WorkerBroadcast(myId = 0, msg = "My_updates", port = 0, counter = count))

        }
      }
  }
}