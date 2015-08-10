import PlayKeys._

name := """securesocial-ws-sample"""

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.5"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"
)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "ws.securesocial" %% "securesocial" % "3.0-M3"
)

routesImport ++= Seq("scala.language.reflectiveCalls")
