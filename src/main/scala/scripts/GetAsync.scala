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
    println(">> DELETING ALL PODS")
    runCmd("kubectl delete service statefulset-service")
    runCmd("kubectl delete statefulset hogwild-pod --cascade=false")
    runCmd("kubectl delete pods --all --grace-period=0 --force")
    println(">> DONE")
  }

  def start(pods: Int): Unit = {
    println(s">> STARTING $pods PODS")
    runCmd("cat async2.yaml" #| s"sed 's/999999/$pods/g'" #| "kubectl create -f -")
    Thread.sleep(10 * 1000)
    println("CATCHING LOGS.....")
    runCmd("kubectl logs hogwild-pod-0 hogwild -f" #> new File(s"async_$pods.log"))
    println(">> DONE")
  }

  def run(): Unit = {
    deletePods()
    Thread.sleep(10 * 1000)
    Seq(1, 3, 4, 7, 11, 17, 27, 41, 64, 100).foreach{ pods =>
      start(pods)
      Thread.sleep(10 * 1000)
      deletePods()
      Thread.sleep(10 * 1000)
    }
  }
}
