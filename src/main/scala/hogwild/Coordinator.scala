package hogwild

import dataset.Dataset
import utils.Label.Label
import utils.Types.TID
import model._
import hogwild.grpc._
import io.grpc.stub.StreamObserver
import utils.WorkingMode
import utils.WorkingMode.WorkingMode

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

object Coordinator extends GrpcServer {

  lazy val samples: Iterator[Int] = Dataset.samples().toIterator

  val lambda: Double = 0.1
  val svm = SVM()

  val slavesHandler = new SlavesHandler

  /* TO COMPUTE & PRINT LOSSES */
  lazy val someDids: Set[TID] = Dataset.didSet.take(500)
  lazy val someFeatures: immutable.IndexedSeq[SparseNumVector] = Dataset.features.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  lazy val someLabels: immutable.IndexedSeq[Label] = Dataset.labels.filter { case (k, v) => someDids(k) }.values.toIndexedSeq
  var i = 0
  var time: Long = System.currentTimeMillis()

  def launch(args: Array[String]): Unit = {
    val workingMode = args(0) match {
      case "sync" => WorkingMode.SYNC
      case "async" => WorkingMode.ASYNC
      case _ => WorkingMode.DEFAULT
    }
    println(s"$workingMode selected")
    if (workingMode == WorkingMode.DEFAULT)
      println("Error unsupported working mode")
    else {
      load()
      startServer(workingMode, 50050)
    }
  }

  def load(): Unit = {
    Dataset.load()
    someDids
    someFeatures
    someLabels
    samples
  }

  def startServer(mode: WorkingMode, port: Int): Unit = {
    val ssd = mode match {
      case WorkingMode.SYNC => WorkerServiceSyncGrpc.bindService(SyncService, ExecutionContext.global)
      case WorkingMode.ASYNC => WorkerServiceAsyncGrpc.bindService(AsyncService, ExecutionContext.global)
    }
    println(">> SERVER READY <<")
    runServer(ssd, port)
  }


  object SyncService extends WorkerServiceSyncGrpc.WorkerServiceSync {

    val instance = this

    override def updateWeights(responseObserver: StreamObserver[WorkerResponse]): StreamObserver[WorkerRequest] =
      new StreamObserver[WorkerRequest] {
        def onError(t: Throwable): Unit = {
          println(s"ON_ERROR: $t")
          slavesHandler.removeSlave()
        }

        def onCompleted(): Unit = {
          println("ON_COMPLETED")
          slavesHandler.removeSlave()
        }

        def onNext(req: WorkerRequest): Unit = {
          if (req.gradient.nonEmpty) {
            instance.synchronized {
              slavesHandler.addGradient(SparseNumVector(req.gradient))
              if(slavesHandler.isWaitingOnSomeSlave){
                instance.wait()
              } else {
                svm.updateWeight(slavesHandler.getMeanGradient)
                if (i % 500 == 0) {
                  val loss = svm.loss(
                    someFeatures,
                    someLabels,
                    lambda,
                    Dataset.tidCounts
                  )
                  val duration = System.currentTimeMillis() - time
                  time = System.currentTimeMillis()
                  println(s"[UPT][$i][$duration]: loss = $loss}")
                }
                i += 1
                instance.notifyAll()
              }
            }
          } else {
            println("[NEW]: a slave wants to compute some gradients")
            slavesHandler.addSlave()
          }
          responseObserver.onNext(spawnWorkerResponse(svm.weights))
        }
      }

    private def spawnWorkerResponse(weights: SparseNumVector): WorkerResponse = {
      val did = samples.next
      WorkerResponse(
        did = did,
        weights = Dataset.getFeature(did).mapTo { case (k, _) => weights.values.withDefaultValue(0d)(k) }.values
      )
    }
  }

  object AsyncService extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    // Stores addresses and ports of running servers
    var currentWorkersAddresses: List[WorkerAddress] = List()

    // Keep count of addresses and ports for when a new worker asks for a port
    var workersAddressesTaken: Map[String, Int] = Map()
    var counter = 0

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

