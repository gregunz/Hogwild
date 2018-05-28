package launcher.mode

import launcher.ArgsHandler.Options
import utils.Logger

case class DefaultMode(options: Options, t: Throwable) extends Mode {
  def run(): Unit = {
    Logger.minimal.alwaysLog(s"ERROR, arguments mismatch: ${t.getMessage}" + "\n\t" + s"with options: $options")
    t.printStackTrace()
  }
}