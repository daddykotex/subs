addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

enablePlugins(SbtTwirl)

organization := "io.github.daddykotex"
name := "subs"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.4"

scalacOptions ++= ScalacOptions.All
// TODO: Try to figure out a way to keep the warn-unused:imports
// scalacOptions in (Compile, TwirlKeys.sourceEncoding) ~= (_ filterNot (_ == "-Ywarn-unused:imports"))
// scalacOptions in (Compile, TwirlKeys.compileTemplates) ~= (_ filterNot (_ == "-Ywarn-unused:imports"))

val Http4sVersion = "0.18.0-M5"
val DoobieVersion = "0.5.0-M9"
val CourierVersion = "1.0.0"

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
  "com.github.daddykotex" %% "courier-core"        % CourierVersion,
  "org.reactormonk"       %% "cryptobits"          % "1.1",
  "org.postgresql"        % "postgresql"           % "42.1.4",
  "ch.qos.logback"        % "logback-classic"      % "1.2.1"
)
