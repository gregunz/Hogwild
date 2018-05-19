package utils

// TODO : use this instead of == "async" in launcher
object WorkingMode extends Enumeration {
  type WorkingMode = Value

  val SYNC: WorkingMode.Value = Value("Synchronous Mode")
  val ASYNC: WorkingMode.Value = Value("Asynchronous Mode")
  val DEFAULT: WorkingMode.Value = Value("default")

  def isSupportedMode(mode: WorkingMode): Boolean = (mode != SYNC || mode != ASYNC)

}
