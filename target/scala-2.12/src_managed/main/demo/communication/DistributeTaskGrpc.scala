package demo.communication

object DistributeTaskGrpc {
  val METHOD_ASK_FOR_TASK: _root_.io.grpc.MethodDescriptor[demo.communication.Task, demo.communication.Task] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("demo.DistributeTask", "askForTask"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(demo.communication.Task))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(demo.communication.Task))
      .build()
  
  val METHOD_RETURN_RESULT: _root_.io.grpc.MethodDescriptor[demo.communication.Task, demo.communication.Task] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("demo.DistributeTask", "returnResult"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(demo.communication.Task))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(demo.communication.Task))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("demo.DistributeTask")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(demo.communication.CommunicationProto.javaDescriptor))
      .addMethod(METHOD_ASK_FOR_TASK)
      .addMethod(METHOD_RETURN_RESULT)
      .build()
  
  trait DistributeTask extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = DistributeTask
    def askForTask(request: demo.communication.Task): scala.concurrent.Future[demo.communication.Task]
    def returnResult(request: demo.communication.Task): scala.concurrent.Future[demo.communication.Task]
  }
  
  object DistributeTask extends _root_.scalapb.grpc.ServiceCompanion[DistributeTask] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[DistributeTask] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = demo.communication.CommunicationProto.javaDescriptor.getServices().get(0)
  }
  
  trait DistributeTaskBlockingClient {
    def serviceCompanion = DistributeTask
    def askForTask(request: demo.communication.Task): demo.communication.Task
    def returnResult(request: demo.communication.Task): demo.communication.Task
  }
  
  class DistributeTaskBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[DistributeTaskBlockingStub](channel, options) with DistributeTaskBlockingClient {
    override def askForTask(request: demo.communication.Task): demo.communication.Task = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_ASK_FOR_TASK, options), request)
    }
    
    override def returnResult(request: demo.communication.Task): demo.communication.Task = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_RETURN_RESULT, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): DistributeTaskBlockingStub = new DistributeTaskBlockingStub(channel, options)
  }
  
  class DistributeTaskStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[DistributeTaskStub](channel, options) with DistributeTask {
    override def askForTask(request: demo.communication.Task): scala.concurrent.Future[demo.communication.Task] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_ASK_FOR_TASK, options), request))
    }
    
    override def returnResult(request: demo.communication.Task): scala.concurrent.Future[demo.communication.Task] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_RETURN_RESULT, options), request))
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): DistributeTaskStub = new DistributeTaskStub(channel, options)
  }
  
  def bindService(serviceImpl: DistributeTask, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_ASK_FOR_TASK,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[demo.communication.Task, demo.communication.Task] {
        override def invoke(request: demo.communication.Task, observer: _root_.io.grpc.stub.StreamObserver[demo.communication.Task]): Unit =
          serviceImpl.askForTask(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .addMethod(
      METHOD_RETURN_RESULT,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[demo.communication.Task, demo.communication.Task] {
        override def invoke(request: demo.communication.Task, observer: _root_.io.grpc.stub.StreamObserver[demo.communication.Task]): Unit =
          serviceImpl.returnResult(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): DistributeTaskBlockingStub = new DistributeTaskBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): DistributeTaskStub = new DistributeTaskStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = demo.communication.CommunicationProto.javaDescriptor.getServices().get(0)
  
}