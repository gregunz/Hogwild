package utils

trait Interval {
  val limit: Int
  private var firstRun: Boolean = true

  def hasReachedOrKeepGoing: Boolean = {
    increase()
    if (firstRun) {
      firstRun = false
      true
    } else {
      val reached = hasReached
      if (reached) {
        reset()
      }
      reached
    }
  }

  def hasReached: Boolean
  def increase(): Unit
  def reset(): Unit
}

case class SecondsInterval(limit: Int) extends Interval {
  private var fromTime: Long = now

  def hasReached: Boolean = timePassed(now) > limit
  def increase(): Unit = {}
  def reset(): Unit = fromTime = now

  private def timePassed(now: Long): Int = ((now - fromTime) / 1000).toInt
  private def now: Long = System.currentTimeMillis()
}

case class IterationsInterval(limit: Int) extends Interval {
  private var counter = 0

  def reset(): Unit = counter = 0
  def hasReached: Boolean = counter >= limit
  def increase(): Unit = counter += 1
}
