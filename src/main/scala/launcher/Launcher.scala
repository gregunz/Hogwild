package launcher

import launcher.mode.Mode

object Launcher {

  def main(args: Array[String]): Unit = {

    val options = ArgsHandler.argsToMap(args)
    val mode = Mode(options)
    mode.run()

  }

}
