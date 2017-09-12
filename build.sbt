enablePlugins(SbtTwirl)

organization := "io.github.daddykotex"
name := "subs"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.3"

val Http4sVersion = "0.18.0-M1"
val DoobieVersion = "0.5.0-M6"

scalafmtOnCompile := true

libraryDependencies ++= Seq(
  "org.http4s"            %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s"            %% "http4s-circe"        % Http4sVersion,
  "org.http4s"            %% "http4s-dsl"          % Http4sVersion,
  "org.http4s"            %% "http4s-twirl"        % Http4sVersion,
  "org.tpolecat"          %% "doobie-core"         % DoobieVersion,
  "org.tpolecat"          %% "doobie-hikari"       % DoobieVersion,
  "org.tpolecat"          %% "doobie-postgres"     % DoobieVersion,
  "org.tpolecat"          %% "doobie-specs2"       % DoobieVersion,
  "io.daddykotex.courier" %% "courier"             % "0.2.0",
  "org.postgresql"        % "postgresql"           % "42.1.4",
  "ch.qos.logback"        % "logback-classic"      % "1.2.1"
)
