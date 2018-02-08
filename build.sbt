name := "downloader"

version := "0.1"

scalaVersion := "2.11.12"

val akkaVersion = "2.5.8"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.log4s" %% "log4s" % "1.3.0"
)
