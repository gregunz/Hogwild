package utils

case class Interval(limit: Int) {
  private var counter = 0

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
}
