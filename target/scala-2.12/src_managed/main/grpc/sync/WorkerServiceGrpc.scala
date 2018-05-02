package grpc.sync

object WorkerServiceGrpc {
  val METHOD_UPDATE_WEIGHTS: _root_.io.grpc.MethodDescriptor[grpc.sync.WorkerBroadcast, grpc.sync.WorkerBroadcast] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.WorkerService", "UpdateWeights"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.sync.WorkerBroadcast))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.sync.WorkerBroadcast))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("grpc.WorkerService")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(grpc.sync.SyncProto.javaDescriptor))
      .addMethod(METHOD_UPDATE_WEIGHTS)
      .build()
  
  trait WorkerService extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = WorkerService
    def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.sync.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[grpc.sync.WorkerBroadcast]
  }
  
  object WorkerService extends _root_.scalapb.grpc.ServiceCompanion[WorkerService] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[WorkerService] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.sync.SyncProto.javaDescriptor.getServices().get(1)
  }
  
  trait WorkerServiceBlockingClient {
    def serviceCompanion = WorkerService
  }
  
  class WorkerServiceBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceBlockingStub](channel, options) with WorkerServiceBlockingClient {
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceBlockingStub = new WorkerServiceBlockingStub(channel, options)
  }
  
  class WorkerServiceStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[WorkerServiceStub](channel, options) with WorkerService {
    override def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.sync.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[grpc.sync.WorkerBroadcast] = {
      _root_.io.grpc.stub.ClientCalls.asyncBidiStreamingCall(channel.newCall(METHOD_UPDATE_WEIGHTS, options), responseObserver)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): WorkerServiceStub = new WorkerServiceStub(channel, options)
  }
  
  def bindService(serviceImpl: WorkerService, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_UPDATE_WEIGHTS,
      _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(new _root_.io.grpc.stub.ServerCalls.BidiStreamingMethod[grpc.sync.WorkerBroadcast, grpc.sync.WorkerBroadcast] {
        override def invoke(observer: _root_.io.grpc.stub.StreamObserver[grpc.sync.WorkerBroadcast]): _root_.io.grpc.stub.StreamObserver[grpc.sync.WorkerBroadcast] =
          serviceImpl.updateWeights(observer)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): WorkerServiceBlockingStub = new WorkerServiceBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): WorkerServiceStub = new WorkerServiceStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.sync.SyncProto.javaDescriptor.getServices().get(1)
  
}