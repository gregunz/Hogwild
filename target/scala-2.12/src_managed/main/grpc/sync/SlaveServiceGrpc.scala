package grpc.sync

object SlaveServiceGrpc {
  val METHOD_UPDATE_WEIGHTS: _root_.io.grpc.MethodDescriptor[grpc.sync.SlaveRequest, grpc.sync.SlaveResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("grpc.SlaveService", "UpdateWeights"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(grpc.sync.SlaveRequest))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(grpc.sync.SlaveResponse))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("grpc.SlaveService")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(grpc.sync.SyncProto.javaDescriptor))
      .addMethod(METHOD_UPDATE_WEIGHTS)
      .build()
  
  trait SlaveService extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = SlaveService
    def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.sync.SlaveResponse]): _root_.io.grpc.stub.StreamObserver[grpc.sync.SlaveRequest]
  }
  
  object SlaveService extends _root_.scalapb.grpc.ServiceCompanion[SlaveService] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[SlaveService] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.sync.SyncProto.javaDescriptor.getServices().get(0)
  }
  
  trait SlaveServiceBlockingClient {
    def serviceCompanion = SlaveService
  }
  
  class SlaveServiceBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[SlaveServiceBlockingStub](channel, options) with SlaveServiceBlockingClient {
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): SlaveServiceBlockingStub = new SlaveServiceBlockingStub(channel, options)
  }
  
  class SlaveServiceStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[SlaveServiceStub](channel, options) with SlaveService {
    override def updateWeights(responseObserver: _root_.io.grpc.stub.StreamObserver[grpc.sync.SlaveResponse]): _root_.io.grpc.stub.StreamObserver[grpc.sync.SlaveRequest] = {
      _root_.io.grpc.stub.ClientCalls.asyncBidiStreamingCall(channel.newCall(METHOD_UPDATE_WEIGHTS, options), responseObserver)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): SlaveServiceStub = new SlaveServiceStub(channel, options)
  }
  
  def bindService(serviceImpl: SlaveService, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_UPDATE_WEIGHTS,
      _root_.io.grpc.stub.ServerCalls.asyncBidiStreamingCall(new _root_.io.grpc.stub.ServerCalls.BidiStreamingMethod[grpc.sync.SlaveRequest, grpc.sync.SlaveResponse] {
        override def invoke(observer: _root_.io.grpc.stub.StreamObserver[grpc.sync.SlaveResponse]): _root_.io.grpc.stub.StreamObserver[grpc.sync.SlaveRequest] =
          serviceImpl.updateWeights(observer)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): SlaveServiceBlockingStub = new SlaveServiceBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): SlaveServiceStub = new SlaveServiceStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = grpc.sync.SyncProto.javaDescriptor.getServices().get(0)
  
}