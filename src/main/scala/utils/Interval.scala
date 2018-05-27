package utils

trait Interval {
  val limit: Int
  private var first = true

  def hasReachedOrFirst: Boolean = {
    if (first) {
      first = false
      true
    } else {
      hasReached
    }
  }

  def hasReached: Boolean

  def reset(): Unit
}