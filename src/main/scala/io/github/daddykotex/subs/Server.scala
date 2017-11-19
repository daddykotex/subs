package io.github.daddykotex.subs

import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.Executors

import cats.effect.IO
import doobie.hikari._
import fs2.Stream
import io.github.daddykotex.subs.repositories.UserRepository
import io.github.daddykotex.subs.web._
import io.github.daddykotex.subs.utils._
import com.github.daddykotex.courier.MailerIO

import org.http4s.util.StreamApp
import org.http4s.util.ExitCode
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

private case class CookieConfig(signingKey: String, cookieName: String)
private object CookieConfig {
  def apply(): CookieConfig =
    envOrNone("COOKIE_SIGNING_KEY")
      .map(sk => CookieConfig(sk, "subs_auth_cookie"))
      .getOrElse { throw new IllegalArgumentException("Cookie configuration environment variables are invalid.") }
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
  private val userRepo = UserRepository.userRepo()

  private val emailConfig = EmailConfig()
  import CatsMailerIO._
  private val mailer = new Mailer[IO](emailConfig)

  private val cookieConfig = CookieConfig()

  private val baseUrl = "http://localhost:8080"
  private val tp = NowTimeProvider
  private val rp = SecureRandomProvider
  private val cs = new DefaultCookieSigner(cookieConfig.signingKey)

  private val authEndpoint =
    new AuthEndpoints[IO]().build(baseUrl, cookieConfig.cookieName, cs, xa, tp, rp, userRepo, mailer)
  private val securedEndpoint = new SecuredEndpoints[IO]().build(cookieConfig.cookieName, cs, xa, userRepo)

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    BlazeBuilder[IO]
      .bindLocal(port)
      .mountService(securedEndpoint)
      .mountService(authEndpoint)
      .withExecutionContext(pool)
      .serve
  }
}

object CatsMailerIO {
  implicit val catsMailerIO: MailerIO[IO] = new MailerIO[IO] {
    def run(f: => Unit): IO[Unit] = IO { f }
  }
}
