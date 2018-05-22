addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.16")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0"