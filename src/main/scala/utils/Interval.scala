package utils

case class Interval(limit: Int, inSecond: Boolean) {
  private var counter = 0

  //TODO: HANDLE INSECOND
  //private var time: Long = System.currentTimeMillis()
  //val duration = ((now - time) / 100d).toInt / 10d
  //time = now

  def increase(): Unit = counter += 1
  def hasReached: Boolean = counter >= limit
  def reset(): Unit = counter = 0

  def resetIfReachedElseIncrease(): Boolean = {
    val reached = hasReached
    if (reached){
      reset()
    } else {
      increase()
    }
    reached
  }

  def prettyLimit: String = limit + {if(inSecond)"sec" else "iterations"}
}
