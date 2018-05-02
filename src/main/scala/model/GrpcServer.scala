package model

import io.grpc.{ServerBuilder, ServerServiceDefinition}

trait GrpcServer {

  /**
    * Just for demo purposes
    */
  def runServer(ssd: ServerServiceDefinition, portNumber: Int): Unit = {
    val server = ServerBuilder
      .forPort(portNumber)
      .addService(ssd)
      .build
      .start

    // make sure our server is stopped when jvm is shut down
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = server.shutdown()
    })

    server.awaitTermination()
  }
}
