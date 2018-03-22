name := "Hogwild"

version := "0.1"

scalaVersion := "2.12.4"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "io.monix" %% "monix" % "2.3.0"
)