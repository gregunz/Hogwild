package hogwild

import java.net._

import dataset.Dataset
import utils.Label.Label
import utils.Types.TID
import model._

import hogwild.grpc._
import io.grpc.stub.StreamObserver
import hogwild.grpc.WorkerServiceSyncGrpc.WorkerServiceSyncStub
import hogwild.grpc.WorkerServiceAsyncGrpc.WorkerServiceAsyncStub
import io.grpc.ManagedChannelBuilder
import utils.WorkingMode
import utils.WorkingMode.WorkingMode

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

object Worker extends GrpcServer {

  private val instance = this
  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val weightsHandler = new WeightsHandler
  val workersHandler = new WorkersHandler

  val lambda: Double = 0.1
  val svm = SVM()

  var counter_tmp = 0

  /* TO COMPUTE & PRINT LOSSES */
  lazy val someDids: Set[TID] = Dataset.didSet.take(500)
  lazy val someFeatures: immutable.IndexedSeq[SparseNumVector] = Dataset.features.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  lazy val someLabels: immutable.IndexedSeq[Label] = Dataset.labels.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  var counts = 0
  var time: Long = System.currentTimeMillis()


  def main(args: Array[String]): Unit = {
    val workingMode = args(0) match {
      case "sync" => WorkingMode.SYNC
      case "async" => WorkingMode.ASYNC
      case _ => WorkingMode.DEFAULT
    }
    println(s"$workingMode selected")

    if (workingMode == WorkingMode.DEFAULT)
      println("Error unsupported working mode")
    else {
      val workerID = args(1).toInt

      if (workingMode == WorkingMode.SYNC) {
        contactCoordinator()
      }
      if (workingMode == WorkingMode.ASYNC) {
        load()
        establishCommunications(workerID)
      }
    }
  }

  def contactCoordinator(): Unit = {

    var someGradient: Option[SparseNumVector] = Some(SparseNumVector.empty)

    val channel = ManagedChannelBuilder
      .forAddress("localhost", 50050) // host and port of service
      .usePlaintext(true) // don't use encryption (for demo purposes)
      .build
    val client: WorkerServiceSyncStub = WorkerServiceSyncGrpc.stub(channel)
    val responseObserver = new StreamObserver[WorkerResponse] {
      def onError(t: Throwable): Unit = {
        println(s"ON_ERROR: $t")
        sys.exit(1)
      }

      def onCompleted(): Unit = {
        println("ON_COMPLETED")
        sys.exit(0)
      }

      def onNext(res: WorkerResponse): Unit = {
        val newGradient = SVM.computeStochasticGradient(
          feature = Dataset.getFeature(res.did),
          label = Dataset.getLabel(res.did),
          weights = SparseNumVector(res.weights),
          lambda = lambda,
          tidCounts = Dataset.tidCounts
        )
        counter_tmp += 1
        if (counter_tmp % 500 == 0) {
          println(s"[CPT]: computing done since start = $counter_tmp)")
        }

        instance.synchronized {
          someGradient = Some(newGradient)
          instance.notifyAll()
        }
      }
    }
    val requestObserver = client.updateWeights(responseObserver)

    // FOR PRINTS

    println(">> SPAWNED <<")

    while (!channel.isTerminated) {
      instance.synchronized {
        while (someGradient.isEmpty) instance.wait()
        requestObserver.onNext(WorkerRequest(someGradient.get.values))
        someGradient = None
      }
    }
  }

  def load(): Unit = {
    Dataset.load()
    someDids
    someFeatures
    someLabels
    samples
  }

  def startServer(port: Int): Unit = {
    val ssd = WorkerServiceAsyncGrpc.bindService(WorkerService, ExecutionContext.global)
    runServer(ssd, port)
  }

  def establishCommunications(workerID : Int): Unit = {

    val coordinatorAddress = "localhost"
    val coordinatorPort = 50050

    // Get local address to send it to the coordinator for port handling
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress: String = localhost.getHostAddress

    // Send local address to coordinator and get unused port in return
    val channel = ManagedChannelBuilder.forAddress(coordinatorAddress, coordinatorPort).usePlaintext(true).build
    val blockingStub = WorkerServiceAsyncGrpc.blockingStub(channel)
    val identificationResponse = blockingStub.identification(InformationRequest(localIpAddress))

    // Run server on port returned
    val serverFuture : Future[Unit] = Future {
      startServer(identificationResponse.port)
    }(ExecutionContext.global)

    // Tell coordinator that server is ready and ask for other worker that are ready
    var addresses = blockingStub.ready(WorkerAddress(localIpAddress, identificationResponse.port))
    // Drop first entry because it is us
    var workerAddresses = addresses.workerAdresses

    // Create a channel for every worker that has a running server
    var channels = workerAddresses.map(x => ManagedChannelBuilder
      .forAddress(x.address, x.port)
      .usePlaintext(true)
      .build)

    var working = true
    while(working){

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
      counts = weightsHandler.getCounts()

      // here we broadcast the weights update
      val n = 500
      if (counts % n == 0) {
        val weigthsUpdateToBroadcast = weightsHandler.getAndResetWeighsUpdate()
        // TODO: SEND GRADIENT TO CHANNELS HERE
        channels.foreach(channel => {
          val client: WorkerServiceAsyncStub = WorkerServiceAsyncGrpc.stub(channel)

          val updateToSend = new StreamObserver[WorkerBroadcast] {
            override def onError(t: Throwable): Unit = {
              println(s"ON_ERROR: $t")
              sys.exit(1)
            }

            override def onCompleted(): Unit = {
              println("ON_COMPLETED")
              sys.exit(0)
            }

            override def onNext(receivedUpdate: WorkerBroadcast): Unit = {
              val receivedWeights = SparseNumVector(receivedUpdate.weightsUpdate)
              svm.addWeightsUpdate(receivedWeights)
            }
          }

          val broadcast = client.updateWeights(updateToSend)
          broadcast.onNext(WorkerBroadcast(Some(weigthsUpdateToBroadcast).get.values, workerID))
          println(s"Update sent to $client")
          // TODO: open channel and send 500 most recent gradient
        })

        println(s"[CPT]: computing done since start = $counts)")
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
  }

  object WorkerService extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    // Stores addresses and ports of running servers
    var currentWorkersAddresses: List[WorkerAddress] = List()

    // Keep count of addresses and ports for when a new worker asks for a port
    var workersAddressesTaken: Map[String, Int] = Map()
    var counter = 0

    //TODO : Handle weight update / gradient and send back gradient before update
    override def updateWeights(responseObserver: StreamObserver[WorkerBroadcast]): StreamObserver[WorkerBroadcast] = {
      new StreamObserver[WorkerBroadcast] {
        override def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          //workersHandler.removeWorker()
        }

        override def onCompleted(): Unit = {
          println("ON_COMPLETED")
          //workersHandler.removeWorker()
        }

        override def onNext(broadcast: WorkerBroadcast): Unit = {
          val receivedId = broadcast.workerId
          println(s"Update received from worker $receivedId")
          svm.addWeightsUpdate(SparseNumVector(broadcast.weightsUpdate))
        }
      }
    }

    override def identification(request: InformationRequest): Future[InformationResponse] = {

      var port = 50051
      if (workersAddressesTaken.contains(request.address)){
        val count = workersAddressesTaken(request.address)
        workersAddressesTaken = workersAddressesTaken + (request.address -> (count + 1))
        port += count
      } else {
        workersAddressesTaken = workersAddressesTaken + (request.address -> 1)
      }

      Future.successful(InformationResponse(port))
    }

    override def ready(request: WorkerAddress): Future[WorkersDetails] = {
      val resp = Future.successful(WorkersDetails(currentWorkersAddresses))
      currentWorkersAddresses ::= request
      resp
    }
  }

  /*
  def getListOfWorkers(map: Map[String, Int]): List[(Any, Int)] = {
    (map.view.map{ case(k,v) => (k, v) } toList).flatMap{case(c, i) => (0 until i).map((c, _)) }
  }
  */
}