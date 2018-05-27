package grpc

import launcher.mode.Mode

trait GrpcRunnable[T <: Mode] {
  def run(mode: T): Unit
}