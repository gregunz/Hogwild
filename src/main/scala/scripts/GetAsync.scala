package scripts

import java.io.File
import scala.sys.process._
import scala.util.Try

object GetAsync {

  private def runCmd(cmd: ProcessBuilder): Try[String] = Try {
    val output: String = {cmd !!}
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

  def start(mode: String, pods: Int, filepath: String): Unit = {
    println(s">> STARTING $pods PODS")
    runCmd(s"cat kubernetes/hogwild_script.yaml" #| s"sed 's/__REPLICAS__/$pods/g'" #| s"sed 's/__MODE__/$mode/g'" #| "kubectl create -f -")
    Thread.sleep(20 * 1000)
    println("CATCHING LOGS.....")
    runCmd("kubectl logs hogwild-pod-0 hogwild -f" #> new File(filepath))
    println(">> DONE")
  }

  def gitPush(filepath: String): Unit = {
    val filename = filepath.substring(filepath.lastIndexOf('/') + 1)
    runCmd(s"git add $filepath" #&& s"git commit -m '$filename added'" #&& "git pull" #&& "git push")
    println(">> DONE")
  }

  def run(mode: String, version: Int = 0): Unit = {
    deletePods()
    (mode match {
      case "sync" => Seq(2, 4, 6, 8, 12, 18, 26, 40, 64)
      case "async" => Seq(1, 2, 4, 6, 8, 12, 18, 26, 40, 64)
    })
      .foreach { pods =>
        val filepath = s"logs/${mode}_${"%03d".format(pods)}_pods_${"%02d".format(version)}.log"
        start(mode, pods, filepath)
        Thread.sleep(2 * 1000)
        gitPush(filepath)
        Thread.sleep(2 * 1000)
        deletePods()
        Thread.sleep(2 * 1000)
      }
  }
}
