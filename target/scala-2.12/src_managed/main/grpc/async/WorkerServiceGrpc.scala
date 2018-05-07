package grpc.async

object WorkerServiceGrpc {
  val METHOD_IDENTIFICATION: _root_.io.grpc.MethodDescriptor[grpc.async.InformationRequest, grpc.async.InformationResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.WorkerService", "Identification"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.async.InformationRequest))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.async.InformationResponse))
      .build()
  
  val METHOD_READY: _root_.io.grpc.MethodDescriptor[grpc.async.WorkerAddress, grpc.async.WorkersDetails] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.WorkerService", "Ready"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.async.WorkerAddress))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.async.WorkersDetails))
      .build()
  
  val METHOD_UPDATE_WEIGHTS: _root_.io.grpc.MethodDescriptor[grpc.async.WorkerBroadcast, grpc.async.WorkerBroadcast] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.WorkerService", "UpdateWeights"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.async.WorkerBroadcast))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.async.WorkerBroadcast))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("grpc.WorkerService")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(grpc.async.AsyncProto.javaDescriptor))
      .addMethod(METHOD_IDENTIFICATION)
      .addMethod(METHOD_READY)
      .addMethod(METHOD_UPDATE_WEIGHTS)
      .build()
  
  trait WorkerService extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = WorkerService
    def identification(request: grpc.async.InformationRequest): scala.concurrent.Future[grpc.async.InformationResponse]
    def ready(request: grpc.async.WorkerAddress): scala.concurrent.Future[grpc.async.WorkersDetails]
    def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.async.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[grpc.async.WorkerBroadcast]
  }
  
  object WorkerService extends _root_.scalapb.grpc.ServiceCompanion[WorkerService] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[WorkerService] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.async.AsyncProto.javaDescriptor.getServices().get(0)
  }
  
  trait WorkerServiceBlockingClient {
    def serviceCompanion = WorkerService
    def identification(request: grpc.async.InformationRequest): grpc.async.InformationResponse
    def ready(request: grpc.async.WorkerAddress): grpc.async.WorkersDetails
  }
  
  class WorkerServiceBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceBlockingStub](channel, options) with WorkerServiceBlockingClient {
    override def identification(request: grpc.async.InformationRequest): grpc.async.InformationResponse = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_IDENTIFICATION, options), request)
    }
    
    override def ready(request: grpc.async.WorkerAddress): grpc.async.WorkersDetails = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_READY, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceBlockingStub = new WorkerServiceBlockingStub(channel, options)
  }
  
  class WorkerServiceStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceStub](channel, options) with WorkerService {
    override def identification(request: grpc.async.InformationRequest): scala.concurrent.Future[grpc.async.InformationResponse] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_IDENTIFICATION, options), request))
    }
    
    override def ready(request: grpc.async.WorkerAddress): scala.concurrent.Future[grpc.async.WorkersDetails] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_READY, options), request))
    }
    
    override def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.async.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[grpc.async.WorkerBroadcast] = {
      _root_.io.grpc.stub.ClientCalls.asyncBidiStreamingCall(channel.newCall(METHOD_UPDATE_WEIGHTS, options), responseObserver)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceStub = new WorkerServiceStub(channel, options)
  }
  
  def bindService(serviceImpl: WorkerService, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_IDENTIFICATION,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[grpc.async.InformationRequest, grpc.async.InformationResponse] {
        override def invoke(request: grpc.async.InformationRequest, observer: _root_.io.grpc.stub.StreamObserver[grpc.async.InformationResponse]): Unit =
          serviceImpl.identification(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .addMethod(
      METHOD_READY,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[grpc.async.WorkerAddress, grpc.async.WorkersDetails] {
        override def invoke(request: grpc.async.WorkerAddress, observer: _root_.io.grpc.stub.StreamObserver[grpc.async.WorkersDetails]): Unit =
          serviceImpl.ready(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .addMethod(
      METHOD_UPDATE_WEIGHTS,
      _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(new _root_.io.grpc.stub.ServerCalls.BidiStreamingMethod[grpc.async.WorkerBroadcast, grpc.async.WorkerBroadcast] {
        override def invoke(observer: _root_.io.grpc.stub.StreamObserver[grpc.async.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[grpc.async.WorkerBroadcast] =
          serviceImpl.updateWeights(observer)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): WorkerServiceBlockingStub = new WorkerServiceBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): WorkerServiceStub = new WorkerServiceStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.async.AsyncProto.javaDescriptor.getServices().get(0)
  
}