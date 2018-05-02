package grpc.async

import model.SparseNumVector

class ConnectionsHandler {
  private var numConnections = 0
  private var portNumbers: Set[Int] = Set()
  private var gradients: List[SparseNumVector] = List()
  private var ids: List[Int] = List()

  def addWorker(portNumber: Int): Unit = {
    numConnections += 1
    portNumbers = portNumbers + portNumber
    println(s"New worker added on port $portNumber")
  }

  def removeWorker(portNumber: Int): Unit = {
    numConnections -= 1
    portNumbers = portNumbers - portNumber
    println(s"Worker on port $portNumber has been removed")
  }

  def getPorts(): Set[Int] = portNumbers

  def isWaitingOnSomeUpdates: Boolean = {
    gradients.size < numConnections
  }

  def addGradient(gradient: SparseNumVector): Unit = {
    gradients ::= gradient
  }

  def addTest(id: Int): Unit = {
    ids ::= id
  }

  def getTest: Int = ids.sum


  def getMeanGradient: SparseNumVector = {
    val meanGradient = gradients.reduce(_ + _).mapTo((k,v) => v / gradients.size)
    gradients = List()
    meanGradient
  }
}
