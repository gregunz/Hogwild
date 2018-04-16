package model


class SlavesHandler {
  private var slaves: Set[Int] = Set()

  def addSlave(id: Int): Unit = {
    slaves += id
  }

  def removeSlave(id: Int): Unit = {
    slaves -= id
  }

  def getSlaves: Set[Int] = slaves
}
