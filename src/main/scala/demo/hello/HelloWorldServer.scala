package demo.hello

import scala.concurrent.{ExecutionContext, Future}

object HelloWorldServer extends GrpcServer {

  def main(args: Array[String]): Unit = {
    val ssd = HelloWorldGrpc.bindService(HelloWorldService, ExecutionContext.global)
    runServer(ssd)
  }

  object HelloWorldService extends HelloWorldGrpc.HelloWorld {
    def sayHello(request: ToBeGreeted): Future[Greeting] = {
      val greetedPerson = request.person match {
        case Some(person) => person.name
        case None => "anonymous"
      }
      Future.successful(Greeting(message = s"Hello ${greetedPerson}!"))
    }
  }

}
