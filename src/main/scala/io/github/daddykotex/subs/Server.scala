package io.github.daddykotex.subs

import java.time.ZoneOffset
import java.util.TimeZone

import cats.effect.IO
import doobie.hikari._
import fs2.Stream
import java.util.concurrent.Executors

import org.http4s.util.StreamApp
import org.http4s.util.StreamApp.ExitCode
import org.http4s.server.blaze.BlazeBuilder

import scala.util.Properties.envOrNone
import scala.concurrent.ExecutionContext

private case class DBConfig(host: String, port: Int, user: String, password: String, dbName: String) {
  val url: String = s"jdbc:postgresql://$host:$port/$dbName"
}
private case class EmailConfig(host: String, port: Int, user: String, password: String)
private object EmailConfig {
  def apply(): EmailConfig =
    (
      for {
        host <- envOrNone("SMTP_HOST")
        port <- envOrNone("SMTP_PORT") map (_.toInt)
        user <- envOrNone("SMTP_USER")
        password <- envOrNone("SMTP_PASS")
      } yield EmailConfig(host, port, user, password)
    ) getOrElse { throw new IllegalArgumentException("SMTP environment variables are invalid.") }
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
        hx setMaximumPoolSize 4
      }
    } yield t
  ).unsafeRunSync()
  private val userRepo = Repositories.userRepo()

  private val emailConfig = EmailConfig()
  import com.github.daddykotex.courier.cats.CatsMailerIO._
  private val mailer = new Mailer[IO](emailConfig)

  private val baseUrl = "http://localhost:8080"
  private val endpoints = new Endpoints[IO]()

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    BlazeBuilder[IO]
      .bindLocal(port)
      .mountService(endpoints.app(baseUrl, xa, NowTimeProvider, SecureRandomProvider, userRepo, mailer))
      .withExecutionContext(pool)
      .serve
  }
}
