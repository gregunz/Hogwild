package grpc.async

import java.net._

import dataset.Dataset
import grpc.GrpcServer
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import launcher.GrpcRunnable
import model._

import scala.concurrent.{ExecutionContext, Future}

object Worker extends GrpcServer with GrpcRunnable {

  type BlockingStub = WorkerServiceAsyncGrpc.WorkerServiceAsyncBlockingStub
  type Stub = WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
  type Worker = (String, Int)
  type Broadcaster = StreamObserver[BroadcastMessage]

  private val instance = this
  var broadcasters: Map[Worker, Broadcaster] = Map.empty

  lazy val myIp: String = InetAddress.getLocalHost.getHostAddress
  var myPort: Int = -1

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val weightsHandler = new WeightsUpdateHandler
  val workersHandler = new ActiveWorkersHandler

  val lambda: Double = 0.1
  val svm = new SVM()

  /* TO COMPUTE & PRINT LOSSES */
  val subsetSize = 500
  lazy val (someFeatures, someLabels) = Dataset.getSubset(subsetSize).unzip
  var time: Long = System.currentTimeMillis()

  def run(args: Seq[String]): Unit = {
    args match {
      case port :: interval :: Nil => // here we assume there are no active workers
        start(myIp, port.toInt, interval.toInt)

      case port :: oneWorkerIp :: oneWorkerPort :: interval :: Nil => // here we know one active workers
        val stub = createBlockingStubToWorker(oneWorkerIp, oneWorkerPort.toInt)
        val (weights, ipAndPortList) = hello(stub)
        svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights

        broadcasters = ipAndPortList.map(v => v -> createBroadcaster(v._1, v._2)).toMap
        start(myIp, port.toInt, interval.toInt)

      case _ =>
        argMismatch(s"expecting oneWorkerIp, oneWorkerPort and myPort but get $args")
    }
  }

  def load(): Unit = {
    samples
    someFeatures
    someLabels
  }

  def start(myIp: String, myPort: Int, broadcastInterval: Int): Unit = {
    this.myPort = myPort
    load()
    spawnWorkingThread(myIp, myPort, broadcastInterval)
    println(">> READY <<")
    startOwnServer(myPort)
  }

  def createBlockingStubToWorker(workerIp: String, workerPort: Int): BlockingStub = {
    val channel: ManagedChannel = ManagedChannelBuilder
      .forAddress(workerIp, workerPort)
      .usePlaintext(true)
      .build

    WorkerServiceAsyncGrpc.blockingStub(channel)
  }

  def createStubToWorker(workerIp: String, workerPort: Int): Stub = {
    val channel = ManagedChannelBuilder
      .forAddress(workerIp, workerPort)
      .usePlaintext(true)
      .build

    WorkerServiceAsyncGrpc.stub(channel)
  }

  def createBroadcaster(workerIp: String, workerPort: Int): Broadcaster = {
    val broadcastObserver = new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = println(s"ON_ERROR: $t")

      override def onCompleted(): Unit = println("ON_COMPLETED")

      override def onNext(msg: Empty): Unit = {
        println("<< SHOULD NOT BE TRIGGERED >>")
      }
    }
    createStubToWorker(workerIp, workerPort).broadcast(broadcastObserver)
  }

  def hello(oneWorkerStub: BlockingStub): (SparseNumVector, Set[(String, Int)]) = {
    val response = oneWorkerStub.hello(Empty())
    SparseNumVector(response.weights) -> response
      .workersDetails
      .map(a => a.address -> a.port.toInt)
      .toSet
  }

  def startOwnServer(myPort: Int): Unit = {
    val ssd = WorkerServiceAsyncGrpc.bindService(WorkerServerService, ExecutionContext.global)
    runServer(ssd, myPort)
  }

  def spawnWorkingThread(myIp: String, myPort: Int, broadcastInterval: Int): Future[Unit] = {
    val myWorkerDetail = WorkerDetail(myIp, myPort)

    Future {
      while (true) {
        val random_did = samples.next
        val newGradient = SVM.computeStochasticGradient(
          feature = Dataset.getFeature(random_did),
          label = Dataset.getLabel(random_did),
          weights = svm.weights,
          lambda = lambda,
          tidCounts = Dataset.tidCounts
        )
        val weightsUpdate = svm.updateWeights(newGradient)
        weightsHandler.addWeightsUpdate(weightsUpdate)
        val counts = weightsHandler.getCounts()

        // here we broadcast the weights update
        if (counts % broadcastInterval == 0) {
          val weigthsUpdateToBroadcast = weightsHandler.getAndResetWeighsUpdate()
          val msg = BroadcastMessage(
            weigthsUpdateToBroadcast.values,
            Some(myWorkerDetail)
          )
          broadcasters.foreach { broadcaster =>
            broadcaster._2.onNext(msg)
            println(s"[SEND] Hei ${broadcaster._1} here are some computations for you <3")
          }

          // compute loss

          //println(s"[CPT]: computing done since start = $counts)")
          val loss = svm.loss(
            someFeatures,
            someLabels,
            lambda,
            Dataset.tidCounts
          )
          val duration = System.currentTimeMillis() - time
          time = System.currentTimeMillis()
          println(s"[UPT][$counts][$duration]: loss = $loss")

        }
      }

    }(ExecutionContext.global)
  }

  object WorkerServerService extends WorkerServiceAsyncGrpc.WorkerServiceAsync {


    override def hello(request: Empty): Future[HelloResponse] = {
      val workersToSend = broadcasters.keySet + (myIp -> myPort)
      val weights = svm.weights

      Future.successful(HelloResponse(
        workersToSend.map { case (ip, port) => WorkerDetail(ip, port) }.toSeq,
        weights.values
      ))
    }

    override def broadcast(responseObserver: StreamObserver[Empty]): StreamObserver[BroadcastMessage] = {
      new StreamObserver[BroadcastMessage] {
        override def onError(t: Throwable): Unit = println(s"ON_ERROR: $t")

        override def onCompleted(): Unit = println("ON_COMPLETED")

        override def onNext(msg: BroadcastMessage): Unit = {
          val detail = msg.workerDetail.get
          val worker = detail.address -> detail.port.toInt

          println(s"[RECEIVED]: Thanks ${worker} for the computation, I owe you some gradients now ;)")
          if (!broadcasters.contains(worker)){
            println(s"[NEW]: New worker just arrived, welcome to the gang $worker")
            addBroadcaster(worker._1, worker._2)
          }

          val receivedWeights = SparseNumVector(msg.weightsUpdate)
          svm.addWeightsUpdate(receivedWeights)
        }
      }
    }

    def addBroadcaster(ip: String, port: Int): Unit = {
      broadcasters += (ip -> port) -> createBroadcaster(ip, port)
    }
  }

}