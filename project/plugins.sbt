addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC10")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.0")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.10")

//TODO: follow https://github.com/playframework/twirl/issues/104 to update to sbt 1.0.0
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.3.4")

resolvers += "Flyway" at "https://flywaydb.org/repo"
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")