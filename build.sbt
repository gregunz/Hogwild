import com.typesafe.sbt.packager.docker.Cmd

name := "Hogwild"

version := "0.1"

scalaVersion := "2.12.6"

enablePlugins(JavaAppPackaging, AshScriptPlugin, sbtdocker.DockerPlugin)


PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
)

mainClass in Compile := Some("launcher.Launcher")

dockerfile in docker := {
  val appDir: File = stage.value
  val targetDir = "/app"
  val username = "daemon"
  val scriptName = "start_node.sh"

  new Dockerfile {
    from("openjdk:jre-alpine")
    add(appDir, targetDir, chown = s"$username:$username")
    workDir(targetDir)
    user(username)
    entryPoint(s"$targetDir/bin/$scriptName")
    cmd()
  }
}
