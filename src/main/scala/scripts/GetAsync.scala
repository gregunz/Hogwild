package scripts

import java.io.File
import scala.sys.process._
import scala.util.Try

object GetAsync {

  private def runCmd(cmd: ProcessBuilder): Try[String] = Try{
    val output: String = (cmd !!)
    println(output)
    output
  }

  def deletePods(): Unit = {
    println("deleting all pods")
    runCmd("kubectl delete service statefulset-service")
    runCmd("kubectl delete statefulset hogwild-pod --cascade=false")
    runCmd("kubectl delete pods --all --grace-period=0 --force")
    println("DONE")
  }

  def start(pods: Int): Unit = {
    println(s"starting $pods pods")
    runCmd("cat async2.yaml" #| s"sed 's/999999/$pods/g'" #| "kubectl create -f -")
    Thread.sleep(10 * 1000)
    println("catching logs")
    runCmd("kubectl logs hogwild-pod-0 hogwild -f" #> new File(s"async_$pods.log"))
    println("DONE")
  }

  def run(): Unit = {
    (2 until 30).foreach{ pods =>
      start(pods)
      Thread.sleep(10 * 1000)
      deletePods()
    }
  }
}
