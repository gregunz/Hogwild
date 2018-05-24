package grpc.async

object WorkerServiceAsyncGrpc {
  val METHOD_HELLO: _root_.io.grpc.MethodDescriptor[grpc.async.WorkerDetail, grpc.async.HelloResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.WorkerServiceAsync", "Hello"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.async.WorkerDetail))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.async.HelloResponse))
      .build()
  
  val METHOD_BROADCAST: _root_.io.grpc.MethodDescriptor[grpc.async.BroadcastMessage, grpc.async.Empty] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.WorkerServiceAsync", "Broadcast"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.async.BroadcastMessage))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.async.Empty))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("grpc.WorkerServiceAsync")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(grpc.async.AsyncProto.javaDescriptor))
      .addMethod(METHOD_HELLO)
      .addMethod(METHOD_BROADCAST)
      .build()
  
  trait WorkerServiceAsync extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = WorkerServiceAsync
    def hello(request: grpc.async.WorkerDetail): scala.concurrent.Future[grpc.async.HelloResponse]
    def broadcast(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.async.Empty]): _root_.io.grpc.stub.StreamObserver[grpc.async.BroadcastMessage]
  }
  
  object WorkerServiceAsync extends _root_.scalapb.grpc.ServiceCompanion[WorkerServiceAsync] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[WorkerServiceAsync] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.async.AsyncProto.javaDescriptor.getServices().get(0)
  }
  
  trait WorkerServiceAsyncBlockingClient {
    def serviceCompanion = WorkerServiceAsync
    def hello(request: grpc.async.WorkerDetail): grpc.async.HelloResponse
  }
  
  class WorkerServiceAsyncBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceAsyncBlockingStub](channel, options) with WorkerServiceAsyncBlockingClient {
    override def hello(request: grpc.async.WorkerDetail): grpc.async.HelloResponse = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_HELLO, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceAsyncBlockingStub = new WorkerServiceAsyncBlockingStub(channel, options)
  }
  
  class WorkerServiceAsyncStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceAsyncStub](channel, options) with WorkerServiceAsync {
    override def hello(request: grpc.async.WorkerDetail): scala.concurrent.Future[grpc.async.HelloResponse] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_HELLO, options), request))
    }
    
    override def broadcast(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.async.Empty]): _root_.io.grpc.stub.StreamObserver[grpc.async.BroadcastMessage] = {
      _root_.io.grpc.stub.ClientCalls.asyncClientStreamingCall(channel.newCall(METHOD_BROADCAST, options), responseObserver)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceAsyncStub = new WorkerServiceAsyncStub(channel, options)
  }
  
  def bindService(serviceImpl: WorkerServiceAsync, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_HELLO,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[grpc.async.WorkerDetail, grpc.async.HelloResponse] {
        override def invoke(request: grpc.async.WorkerDetail, observer: _root_.io.grpc.stub.StreamObserver[grpc.async.HelloResponse]): Unit =
          serviceImpl.hello(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .addMethod(
      METHOD_BROADCAST,
      _root_.io.grpc.stub.ServerCalls.asyncClientStreamingCall(new _root_.io.grpc.stub.ServerCalls.ClientStreamingMethod[grpc.async.BroadcastMessage, grpc.async.Empty] {
        override def invoke(observer: _root_.io.grpc.stub.StreamObserver[grpc.async.Empty]): _root_.io.grpc.stub.StreamObserver[grpc.async.BroadcastMessage] =
          serviceImpl.broadcast(observer)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): WorkerServiceAsyncBlockingStub = new WorkerServiceAsyncBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): WorkerServiceAsyncStub = new WorkerServiceAsyncStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.async.AsyncProto.javaDescriptor.getServices().get(0)
  
}