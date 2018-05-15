package hogwild

object Launcher {

  def main(args: Array[String]): Unit = {
    while (args.length != 2) {
      println("Please enter a working mode (sync or async) " +
        "and a node type (coord or worker)")
      sys.exit()
    }

    val workingMode = args(0)
    val nodeType = args(1)

    if (workingMode == "async") {
      println("Asynchronous mode selected")
      Worker.main(Array(workingMode, nodeType))

    } else if (workingMode == "sync") {
      println("Synchronous mode selected")

    } else {

    }

  }


}
