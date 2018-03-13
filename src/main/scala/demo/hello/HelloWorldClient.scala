package demo.hello

import demo.hello.HelloWorldGrpc.{HelloWorldBlockingStub, HelloWorldStub}
import demo.hello.ToBeGreeted.Person
import io.grpc.ManagedChannelBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object HelloWorldClient extends App {

  val channel = ManagedChannelBuilder
    .forAddress("localhost", 50051) // host and port of service
    .usePlaintext(true) // don't use encryption (for demo purposes)
    .build

  val person = Person(
    name = "Bob"
  )

  val toBeGreeted = ToBeGreeted(Some(person))
  // or use the generated builder methods
  // val toBeGreeted2 = ToBeGreeted().withPerson(Person(name = "Bob"))

  // async client
  val stub: HelloWorldStub = HelloWorldGrpc.stub(channel)
  val greetingF: Future[Greeting] = stub.sayHello(toBeGreeted)

  greetingF.foreach(response => println(s"ASYNC RESULT: ${response.message}"))

  // beware: blocking code below
  val blockingStub: HelloWorldBlockingStub = HelloWorldGrpc.blockingStub(channel)
  val greeting: Greeting = blockingStub.sayHello(toBeGreeted)

  println(s"SYNC(BLOCKING) RESULT: ${greeting.message}")

}
