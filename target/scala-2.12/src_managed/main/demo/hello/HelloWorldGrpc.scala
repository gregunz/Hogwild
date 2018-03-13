package demo.hello

object HelloWorldGrpc {
  val METHOD_SAY_HELLO: _root_.io.grpc.MethodDescriptor[demo.hello.ToBeGreeted, demo.hello.Greeting] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("demo.HelloWorld", "SayHello"))
      .setRequestMarshaller(new scalapb.grpc.Marshaller(demo.hello.ToBeGreeted))
      .setResponseMarshaller(new scalapb.grpc.Marshaller(demo.hello.Greeting))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("demo.HelloWorld")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(demo.hello.HelloProto.javaDescriptor))
      .addMethod(METHOD_SAY_HELLO)
      .build()
  
  trait HelloWorld extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = HelloWorld
    def sayHello(request: demo.hello.ToBeGreeted): scala.concurrent.Future[demo.hello.Greeting]
  }
  
  object HelloWorld extends _root_.scalapb.grpc.ServiceCompanion[HelloWorld] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[HelloWorld] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = demo.hello.HelloProto.javaDescriptor.getServices().get(0)
  }
  
  trait HelloWorldBlockingClient {
    def serviceCompanion = HelloWorld
    def sayHello(request: demo.hello.ToBeGreeted): demo.hello.Greeting
  }
  
  class HelloWorldBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[HelloWorldBlockingStub](channel, options) with HelloWorldBlockingClient {
    override def sayHello(request: demo.hello.ToBeGreeted): demo.hello.Greeting = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_SAY_HELLO, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): HelloWorldBlockingStub = new HelloWorldBlockingStub(channel, options)
  }
  
  class HelloWorldStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[HelloWorldStub](channel, options) with HelloWorld {
    override def sayHello(request: demo.hello.ToBeGreeted): scala.concurrent.Future[demo.hello.Greeting] = {
      scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_SAY_HELLO, options), request))
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): HelloWorldStub = new HelloWorldStub(channel, options)
  }
  
  def bindService(serviceImpl: HelloWorld, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_SAY_HELLO,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[demo.hello.ToBeGreeted, demo.hello.Greeting] {
        override def invoke(request: demo.hello.ToBeGreeted, observer: _root_.io.grpc.stub.StreamObserver[demo.hello.Greeting]): Unit =
          serviceImpl.sayHello(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): HelloWorldBlockingStub = new HelloWorldBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): HelloWorldStub = new HelloWorldStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = demo.hello.HelloProto.javaDescriptor.getServices().get(0)
  
}