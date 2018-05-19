package hogwild.grpc

object WorkerServiceAsyncGrpc {
  val METHOD_IDENTIFICATION: _root_.io.grpc.MethodDescriptor[hogwild.grpc.InformationRequest, hogwild.grpc.InformationResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("hogwild.WorkerServiceAsync", "Identification"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.InformationRequest))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.InformationResponse))
      .build()
  
  val METHOD_READY: _root_.io.grpc.MethodDescriptor[hogwild.grpc.WorkerAddress, hogwild.grpc.WorkersDetails] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("hogwild.WorkerServiceAsync", "Ready"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.WorkerAddress))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.WorkersDetails))
      .build()
  
  val METHOD_UPDATE_WEIGHTS: _root_.io.grpc.MethodDescriptor[hogwild.grpc.WorkerBroadcast, hogwild.grpc.WorkerBroadcast] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("hogwild.WorkerServiceAsync", "UpdateWeights"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.WorkerBroadcast))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.WorkerBroadcast))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("hogwild.WorkerServiceAsync")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(hogwild.grpc.GrpcProto.javaDescriptor))
      .addMethod(METHOD_IDENTIFICATION)
      .addMethod(METHOD_READY)
      .addMethod(METHOD_UPDATE_WEIGHTS)
      .build()
  
  trait WorkerServiceAsync extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = WorkerServiceAsync
    def identification(request: hogwild.grpc.InformationRequest): scala.concurrent.Future[hogwild.grpc.InformationResponse]
    def ready(request: hogwild.grpc.WorkerAddress): scala.concurrent.Future[hogwild.grpc.WorkersDetails]
    def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerBroadcast]
  }
  
  object WorkerServiceAsync extends _root_.scalapb.grpc.ServiceCompanion[WorkerServiceAsync] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[WorkerServiceAsync] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = hogwild.grpc.GrpcProto.javaDescriptor.getServices().get(1)
  }
  
  trait WorkerServiceAsyncBlockingClient {
    def serviceCompanion = WorkerServiceAsync
    def identification(request: hogwild.grpc.InformationRequest): hogwild.grpc.InformationResponse
    def ready(request: hogwild.grpc.WorkerAddress): hogwild.grpc.WorkersDetails
  }
  
  class WorkerServiceAsyncBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceAsyncBlockingStub](channel, options) with WorkerServiceAsyncBlockingClient {
    override def identification(request: hogwild.grpc.InformationRequest): hogwild.grpc.InformationResponse = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_IDENTIFICATION, options), request)
    }
    
    override def ready(request: hogwild.grpc.WorkerAddress): hogwild.grpc.WorkersDetails = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_READY, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceAsyncBlockingStub = new WorkerServiceAsyncBlockingStub(channel, options)
  }
  
  class WorkerServiceAsyncStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceAsyncStub](channel, options) with WorkerServiceAsync {
    override def identification(request: hogwild.grpc.InformationRequest): scala.concurrent.Future[hogwild.grpc.InformationResponse] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_IDENTIFICATION, options), request))
    }
    
    override def ready(request: hogwild.grpc.WorkerAddress): scala.concurrent.Future[hogwild.grpc.WorkersDetails] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_READY, options), request))
    }
    
    override def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerBroadcast] = {
      _root_.io.grpc.stub.ClientCalls.asyncBidiStreamingCall(channel.newCall(METHOD_UPDATE_WEIGHTS, options), responseObserver)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceAsyncStub = new WorkerServiceAsyncStub(channel, options)
  }
  
  def bindService(serviceImpl: WorkerServiceAsync, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_IDENTIFICATION,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[hogwild.grpc.InformationRequest, hogwild.grpc.InformationResponse] {
        override def invoke(request: hogwild.grpc.InformationRequest, observer: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.InformationResponse]): Unit =
          serviceImpl.identification(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .addMethod(
      METHOD_READY,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[hogwild.grpc.WorkerAddress, hogwild.grpc.WorkersDetails] {
        override def invoke(request: hogwild.grpc.WorkerAddress, observer: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkersDetails]): Unit =
          serviceImpl.ready(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .addMethod(
      METHOD_UPDATE_WEIGHTS,
      _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(new _root_.io.grpc.stub.ServerCalls.BidiStreamingMethod[hogwild.grpc.WorkerBroadcast, hogwild.grpc.WorkerBroadcast] {
        override def invoke(observer: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerBroadcast] =
          serviceImpl.updateWeights(observer)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): WorkerServiceAsyncBlockingStub = new WorkerServiceAsyncBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): WorkerServiceAsyncStub = new WorkerServiceAsyncStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = hogwild.grpc.GrpcProto.javaDescriptor.getServices().get(1)
  
}