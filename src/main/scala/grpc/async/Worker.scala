package grpc.async

import java.net._

import dataset.Dataset
import grpc.async.BroadcastersHandler.RemoteWorker
import grpc.{GrpcRunnable, GrpcServer}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import model._
import utils.{AsyncWorkerMode, Interval}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object Worker extends GrpcServer with GrpcRunnable[AsyncWorkerMode] {

  private val broadcastersHandler = BroadcastersHandler

  def run(mode: AsyncWorkerMode): Unit = {

    val dataset = Dataset(mode.dataPath, mode.samples).fullLoad()
    val svm = new SVM(lambda = mode.lambda, stepSize = mode.stepSize)
    val myIp: String = InetAddress.getLocalHost.getHostAddress
    val myPort = mode.port

    startServer(myIp, myPort, svm)

    if (mode.worker.isDefined) {
      val worker = mode.worker.get
      val channel = createChannel(worker)
      val (weights, workers) = hello(myIp, myPort, worker, channel)
      svm.addWeightsUpdate(weights) // adding update when we are at zero is like setting weights

      broadcastersHandler.addSomeActive(workers)
    }

    startComputations(myIp, myPort, dataset, svm, mode.interval, mode.isMaster)
  }

  def createChannel(worker: RemoteWorker): ManagedChannel = {
    ManagedChannelBuilder
      .forAddress(worker.ip, worker.port)
      .usePlaintext(true)
      .build
  }

  def hello(myIp: String, myPort: Int, worker: RemoteWorker, channel: ManagedChannel): (SparseNumVector[Double], Set[RemoteWorker]) = {
    val stub = WorkerServiceAsyncGrpc.blockingStub(channel)
    val response = stub.hello(WorkerDetail(myIp, myPort))

    SparseNumVector(response.weights) -> response
      .workersDetails
      .map(a => RemoteWorker(a.address, a.port.toInt))
      .toSet
  }

  def startServer(myIp: String, myPort: Int, svm: SVM): Unit = {
    val ssd = WorkerServiceAsyncGrpc.bindService(WorkerServerService(myIp, myPort, svm), ExecutionContext.global)
    println(">> Server starting..")
    runServer(ssd, myPort)
  }

  def startComputations(myIp: String, myPort: Int, dataset: Dataset, svm: SVM, interval: Interval,
                        isMaster: Boolean): Unit = {
    val myWorkerDetail = WorkerDetail(myIp, myPort)
    val samples: Iterator[Int] = dataset.samples().toIterator
    val stoppingCriterion = StoppingCriterion(dataset)


    println(">> Computations thread starting..")

    while (true) {
      val random_did = samples.next
      val newGradient = svm.computeStochasticGradient(
        feature = dataset.getFeature(random_did),
        label = dataset.getLabel(random_did),
        tidCounts = dataset.tidCounts
      )
      val weightsUpdate = svm.updateWeights(newGradient)
      WeightsUpdateHandler.addWeightsUpdate(weightsUpdate)

      if (interval.resetIfReachedElseIncrease()) {
        val msg = BroadcastMessage(
          WeightsUpdateHandler.getAndResetWeightsUpdate().toMap,
          Some(myWorkerDetail)
        )
        broadcastersHandler.broadcast(msg)
        if(isMaster){
          stoppingCriterion.compute(svm, displayLoss = true)
          if (stoppingCriterion.shouldStop){
            broadcastersHandler.killAll()
            WeightsExport.uploadWeightsAndGetLink(svm.weights)
            sys.exit(0)
          }
        }
      }
    }
  }

  case class WorkerServerService(myIp: String, myPort: Int, svm: SVM) extends WorkerServiceAsyncGrpc.WorkerServiceAsync {

    override def hello(request: WorkerDetail): Future[HelloResponse] = {
      val workersToSend = broadcastersHandler.activeWorkers + RemoteWorker(myIp, myPort)
      val weights = svm.weights

      broadcastersHandler.addToWaitingList(RemoteWorker(request.address, request.port.toInt))

      Future(HelloResponse(
        workersToSend.map { w => WorkerDetail(w.ip, w.port) }.toSeq,
        weights.toMap
      ))
    }

    override def broadcast(responseObserver: StreamObserver[Empty]): StreamObserver[BroadcastMessage] = {
      new StreamObserver[BroadcastMessage] {
        override def onError(t: Throwable): Unit = {}

        override def onCompleted(): Unit = {}

        override def onNext(msg: BroadcastMessage): Unit = {
          val detail = msg.workerDetail.get
          val worker = RemoteWorker(detail.address, detail.port.toInt)

          broadcastersHandler.add(worker)
          println(s"[RECEIVED]: thanks to $worker for the computation, I owe you some gradients now ;)")

          val receivedWeights = SparseNumVector(msg.weightsUpdate)
          svm.addWeightsUpdate(receivedWeights)
        }
      }
    }

    override def kill(request: Empty): Future[Empty] = {
      println(s"[KILLED]: this is the end, my friend... i am proud to have served you... arrrrghhh... (dying alone on the field)")
      sys.exit(0)
      Future(Empty())
    }
  }

}