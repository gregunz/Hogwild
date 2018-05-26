package grpc

import launcher.Mode

trait GrpcRunnable[T <: Mode] {
  def run(mode: T): Unit
}