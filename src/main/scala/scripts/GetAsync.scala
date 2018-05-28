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

  def start(pods: Int, i: Int): Unit = {
    println(s">> STARTING $pods PODS")
    runCmd("cat kubernetes/async_script.yaml" #| s"sed 's/999999/$pods/g'" #| "kubectl create -f -")
    Thread.sleep(10 * 1000)
    println("CATCHING LOGS.....")
    runCmd("kubectl logs hogwild-pod-0 hogwild -f" #> new File(s"logs/async_${"%03d".format(pods)}_pods_${"%02d".format(i)}.log"))
    println(">> DONE")
  }

  def run(i: Int = 0): Unit = {
    deletePods()
    Seq(1, 2, 4, 6, 8, 12, 18, 26, 40, 64).foreach{ pods =>
      Thread.sleep(10 * 1000)
      start(pods, i)
      Thread.sleep(10 * 1000)
      deletePods()
    }
  }
}
