package launcher

trait GrpcRunnable {
  def run(args: Seq[String] = Seq.empty): Unit

  def argMismatch(msg: String = ""): Unit = {
    println(s"argument mismatch: '$msg'")
    sys.exit(1)
  }
}