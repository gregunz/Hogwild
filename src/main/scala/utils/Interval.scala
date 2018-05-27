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