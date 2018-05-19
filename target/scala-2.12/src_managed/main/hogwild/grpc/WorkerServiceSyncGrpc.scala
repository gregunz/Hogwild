package hogwild.grpc

object WorkerServiceSyncGrpc {
  val METHOD_UPDATE_WEIGHTS: _root_.io.grpc.MethodDescriptor[hogwild.grpc.WorkerRequest, hogwild.grpc.WorkerResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("hogwild.WorkerServiceSync", "UpdateWeights"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.WorkerRequest))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(hogwild.grpc.WorkerResponse))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("hogwild.WorkerServiceSync")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(hogwild.grpc.GrpcProto.javaDescriptor))
      .addMethod(METHOD_UPDATE_WEIGHTS)
      .build()
  
  trait WorkerServiceSync extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = WorkerServiceSync
    def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerResponse]): _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerRequest]
  }
  
  object WorkerServiceSync extends _root_.scalapb.grpc.ServiceCompanion[WorkerServiceSync] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[WorkerServiceSync] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = hogwild.grpc.GrpcProto.javaDescriptor.getServices().get(0)
  }
  
  trait WorkerServiceSyncBlockingClient {
    def serviceCompanion = WorkerServiceSync
  }
  
  class WorkerServiceSyncBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceSyncBlockingStub](channel, options) with WorkerServiceSyncBlockingClient {
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceSyncBlockingStub = new WorkerServiceSyncBlockingStub(channel, options)
  }
  
  class WorkerServiceSyncStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceSyncStub](channel, options) with WorkerServiceSync {
    override def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerResponse]): _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerRequest] = {
      _root_.io.grpc.stub.ClientCalls.asyncBidiStreamingCall(channel.newCall(METHOD_UPDATE_WEIGHTS, options), responseObserver)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceSyncStub = new WorkerServiceSyncStub(channel, options)
  }
  
  def bindService(serviceImpl: WorkerServiceSync, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_UPDATE_WEIGHTS,
      _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(new _root_.io.grpc.stub.ServerCalls.BidiStreamingMethod[hogwild.grpc.WorkerRequest, hogwild.grpc.WorkerResponse] {
        override def invoke(observer: _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerResponse]): _root_.io.grpc.stub.StreamObserver[hogwild.grpc.WorkerRequest] =
          serviceImpl.updateWeights(observer)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): WorkerServiceSyncBlockingStub = new WorkerServiceSyncBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): WorkerServiceSyncStub = new WorkerServiceSyncStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = hogwild.grpc.GrpcProto.javaDescriptor.getServices().get(0)
  
}