enablePlugins(SbtTwirl)

organization := "io.github.daddykotex"
name := "subs"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.3"

val Http4sVersion = "0.17.0-RC1"
val CatVersion = "0.4.4"

scalafmtOnCompile := true

libraryDependencies ++= Seq(
  "org.http4s"     %% "http4s-blaze-server"  % Http4sVersion,
  "org.http4s"     %% "http4s-circe"         % Http4sVersion,
  "org.http4s"     %% "http4s-dsl"           % Http4sVersion,
  "org.http4s"     %% "http4s-twirl"         % Http4sVersion,
  "org.tpolecat"   %% "doobie-core-cats"     % CatVersion,
  "org.tpolecat"   %% "doobie-postgres-cats" % CatVersion,
  "ch.qos.logback" % "logback-classic"       % "1.2.1"
)
