package utils

case class SecondsInterval(limit: Int) extends Interval {
  private var fromTime: Long = now

  def hasReached: Boolean = timePassed(now) > limit

  private def timePassed(now: Long): Int = ((now - fromTime) / 1000).toInt

  private def now: Long = System.currentTimeMillis()

  def reset(): Unit = fromTime = now
}
