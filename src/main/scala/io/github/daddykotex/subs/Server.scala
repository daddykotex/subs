package io.github.daddykotex.subs

import java.time.ZoneOffset
import java.util.TimeZone

import cats.effect.IO
import cats.implicits._
import doobie.hikari._
import doobie.hikari.implicits._
import fs2.Stream
import java.util.concurrent.Executors

import org.http4s.util.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import scala.util.Properties.envOrNone
import scala.concurrent.ExecutionContext

private case class DBConfig(host: String, port: Int, user: String, password: String, dbName: String) {
  val url: String = s"jdbc:postgresql://$host:$port/$dbName"
}
private object DBConfig {
  def apply(): DBConfig =
    (
      for {
        host <- envOrNone("DB_HOST")
        port <- envOrNone("DB_PORT") map (_.toInt)
        dbName <- envOrNone("DB_NAME")
        user <- envOrNone("DB_USER")
        password <- envOrNone("DB_PASS")
      } yield DBConfig(host, port, user, password, dbName)
    ) getOrElse { throw new IllegalArgumentException("Database environment variables are invalid.") }
}

object Server extends StreamApp[IO] {
  TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC.getId))

  private val port: Int = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
  private val dbConfig = DBConfig()

  private val pool: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val xa = (
    for {
      t <- HikariTransactor[IO]("org.postgresql.Driver", dbConfig.url, dbConfig.user, dbConfig.password)
      _ <- t.configure { hx =>
        hx setMaximumPoolSize 2
      }
    } yield t
  ).unsafeRunSync()
  private val userRepo = Repositories.userRepo()

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, Nothing] = {
    BlazeBuilder[IO]
      .bindLocal(port)
      .mountService(Web.app(xa, TimeProvider.now(), userRepo))
      .withExecutionContext(pool)
      .serve
  }
}
