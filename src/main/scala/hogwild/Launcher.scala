package hogwild

import utils.WorkingMode

object Launcher {

  def main(args: Array[String]): Unit = {
    while (args.length != 2) {
      println("Please enter a working mode (sync or async) " +
        "and a node type (coord or worker)")
      sys.exit()
    }

    val workingMode = args(0)
    val nodeType = args(1)

    var workerID = 1

    if (nodeType == "coord") {
      Coordinator.main(Array(workingMode))
    } else if (nodeType == "worker") {
      Worker.main(Array(workingMode, workerID.toString))
      workerID += 1
    }
  }


}
