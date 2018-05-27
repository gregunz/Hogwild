package utils

case class IterationsInterval(limit: Int) extends Interval {
  private var counter = 0

  def reset(): Unit = counter = 0

  def hasReached: Boolean = {
    val reached = counter >= limit
    counter += 1
    reached
  }

}

