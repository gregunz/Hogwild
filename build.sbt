import com.typesafe.sbt.packager.docker.Cmd

name := "Hogwild"

version := "0.1"

scalaVersion := "2.12.6"

// Increase Java Max Heap Size (https://goo.gl/wuBkQM)
javaOptions += "-Xmx20G"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
)

mainClass in Compile := Some("launcher.Launcher")

//dockerBaseImage := "openjdk:jre-alpine"
//dockerEntrypoint := Seq("/opt/docker/bin/start_node.sh")

// hardcoding the docker image we want
dockerCommands := Seq(
  Cmd("FROM", "openjdk:jre-alpine"),
  Cmd("ADD", "opt", "/opt"),
  Cmd("WORKDIR", "/opt/docker/bin"),
  Cmd("RUN", """["chmod", "+x", "hogwild"]"""),
  Cmd("RUN", """["chmod", "+x", "start_node.sh"]"""),
  Cmd("ENTRYPOINT", """["sh" , "start_node.sh"]"""),
  Cmd("CMD", "[]"),
)
