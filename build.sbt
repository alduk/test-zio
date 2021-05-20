name := "test-zio"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-streams" % "1.0.5",
  "dev.zio" %% "zio-kafka"   % "0.14.0",
  "io.d11" %% "zhttp" % "1.0.0.0-RC13",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "dev.zio" %% "zio-logging-slf4j" % "0.5.8"
)