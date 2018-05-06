package model

class WorkersHandler {
  private var portNumbers: Set[Int] = Set()

  def addWorker(address: String, portNumber: Int): Unit = {
    portNumbers = portNumbers + portNumber
    println(s"New worker added on port $portNumber")
  }

  def removeWorker(address: String, portNumber: Int): Unit = {
    portNumbers = portNumbers - portNumber
    println(s"Worker on port $portNumber has been removed")
  }

  def getPorts: Set[Int] = portNumbers
}
