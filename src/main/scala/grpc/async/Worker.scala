package grpc.async

import java.net._

import dataset.Dataset
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import launcher.AsyncWorkerMode
import model._
import utils.Types.TID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Try}

object Worker extends GrpcServer with GrpcRunnable[AsyncWorkerMode] {

  private var keepComputing = true

  def run(mode: AsyncWorkerMode): Unit = {

    val dataset = mode.dataset.getReady(mode.isMaster)
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val myIp: String = InetAddress.getLocalHost.getHostAddress
    val myPort = mode.port

    val broadcastersHandler: BroadcastersHandler = Try {
        val (meWorker, weights, workers) = hello(myIp, myPort, mode.workerIp, mode.workerPort)
        println(s"I AM $meWorker")
        val broadcastersHandler = BroadcastersHandler(dataset, meWorker, mode.broadcastInterval)
        svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights
        broadcastersHandler.addSomeActive(workers)
        broadcastersHandler
    }.getOrElse({
      if (mode.isSlave){
        throw new IllegalStateException(s"Failed to connect to ${mode.workerIp}:${mode.workerPort}")
      }
      println(s"I AM MASTER")
      BroadcastersHandler(mode.dataset, RemoteWorker(myIp, myPort, 0), mode.broadcastInterval)
    })

    startServer(svm, broadcastersHandler)
    startComputations(dataset, svm, broadcastersHandler, mode.stoppingCriteria)

  }

  def hello(myIp: String, myPort: Int, workerIp: String, workerPort: Int): (RemoteWorker, SparseNumVector[Double], Set[RemoteWorker]) = {
    val channel = createChannel(workerIp, workerPort)
    val stub = WorkerServiceAsyncGrpc.blockingStub(channel)
    val response = stub.hello(HelloRequest(ip = myIp, port = myPort))

    val me = RemoteWorker(ip = myIp, port = myPort, id = response.id)
    val weights = SparseNumVector(response.weights)
    val workers = response
      .workersDetails
      .map(RemoteWorker.fromWorkerDetails)
      .toSet

    (me, weights, workers)
  }

  private def createChannel(ip: String, port: Int): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(ip, port)
      .usePlaintext(true)
      .build
  }

  def startServer(svm: SVM, broadcastersHandler: BroadcastersHandler): Unit = {
    val ssd = WorkerServiceAsyncGrpc.bindService(WorkerServerService(svm, broadcastersHandler), ExecutionContext.global)
    println(">> Server starting..")
    runServer(ssd, broadcastersHandler.meWorker.port)
  }

  def startComputations(dataset: Dataset, svm: SVM, broadcastersHandler: BroadcastersHandler,
                        someStoppingCriteria: Option[StoppingCriteria]): Unit = {


    println(">> Computations thread starting..")

    var broadcastFuture = Future.successful()
    var lossComputingFuture = Future.successful()

    while (keepComputing && someStoppingCriteria.forall(!_.shouldStop)) {
      val (feature, label) = dataset.getSample
      val newGradient = svm.computeStochasticGradient(
        feature = feature,
        label = label,
        inverseTidCountsVector = dataset.inverseTidCountsVector
      )
      val weightsUpdate = svm.updateWeights(newGradient)
      WeightsUpdateHandler.addWeightsUpdate(weightsUpdate)

      if (broadcastersHandler.broadcastInterval.hasReachedOrKeepGoing && broadcastFuture.isCompleted) {
        broadcastFuture = Future {
          val weights = WeightsUpdateHandler.getAndResetWeightsUpdate()
          broadcastersHandler.broadcast(weights)
        }
      }
      someStoppingCriteria.foreach{ stoppingCriteria =>
        if (stoppingCriteria.interval.hasReachedOrKeepGoing && lossComputingFuture.isCompleted){
          lossComputingFuture = Future{
            stoppingCriteria.compute(svm, displayLoss = true)
          }
        }
      }
    }
    if (someStoppingCriteria.isDefined){
      broadcastersHandler.killAll()
      someStoppingCriteria.get.export()
    }

    Thread.sleep(Long.MaxValue)
  }

  def tidsToBroadcast(dataset: Dataset, i: Int, n: Int): Set[TID] = {
    val allTids = dataset.inverseTidCountsVector.keys.toSeq.sorted
    allTids.filter(tid => tid % n == i).toSet
  }

  case class WorkerServerService(svm: SVM, broadcastersHandler: BroadcastersHandler)
    extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    @scala.annotation.tailrec
    final def generateId(takenIds: Set[Int]): Int = {
      val workerId = Random.nextInt(Int.MaxValue)
      if (takenIds(workerId)) {
        generateId(takenIds)
      } else {
        workerId
      }
    }

    override def hello(request: HelloRequest): Future[HelloResponse] = {
      val workersToSend = broadcastersHandler.allWorkers
      val takenIds = workersToSend.map(_.id)
      val newWorker = RemoteWorker(ip = request.ip, port = request.port, generateId(takenIds))
      val weights = svm.weights

      broadcastersHandler.addToWaitingList(newWorker)

      Future(HelloResponse(
        workersToSend.map { w => WorkerDetail(w.ip, w.port) }.toSeq,
        weights.toMap,
        id = newWorker.id
      ))
    }

    override def broadcast(responseObserver: StreamObserver[Empty]): StreamObserver[BroadcastMessage] = {
      new StreamObserver[BroadcastMessage] {
        override def onError(t: Throwable): Unit = {}

        override def onCompleted(): Unit = {}

        override def onNext(msg: BroadcastMessage): Unit = {
          val worker = RemoteWorker.fromWorkerDetails(msg.workerDetail.get)

          broadcastersHandler.add(worker)
          println(s"[RECEIVED]: thanks to $worker for the computation, I owe you some gradients now ;)")

          val receivedWeights = SparseNumVector(msg.weightsUpdate)
          svm.addWeightsUpdate(receivedWeights)
        }
      }
    }

    override def kill(request: Empty): Future[Empty] = {
      println(s"[KILLED]: this is the end, my friend... i am proud to have served you... arrrrghhh... (dying alone on the field)")
      keepComputing = false
      //sys.exit(0)
      Future(Empty())
    }
  }

}