package io.github.daddykotex.subs

import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.Executors

import cats.effect.IO
import doobie.hikari._
import fs2.Stream
import fs2.StreamApp, StreamApp.ExitCode
import io.github.daddykotex.subs.repositories.UserRepository
import io.github.daddykotex.subs.web._
import io.github.daddykotex.subs.utils._
import org.http4s.Uri
import com.github.daddykotex.courier.MailerIO

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.client.blaze._

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
private case class MailgunConfig(baseUri: Uri, key: String)
private object MailgunConfig {
  def apply(): MailgunConfig =
    (
      for {
        uri <- envOrNone("MAILGUN_HOST")
        validUri <- Uri.fromString(uri).toOption
        key <- envOrNone("MAILGUN_KEY")
      } yield MailgunConfig(validUri, key)
    ) getOrElse { throw new IllegalArgumentException("MailGun environment variables are invalid.") }
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

  private implicit val pool: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  private val emailConfig = EmailConfig()
  import CatsMailerIO._
  private val mailer = new Mailer[IO](emailConfig)

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    for {
      xa <- Stream.eval {
        HikariTransactor
          .newHikariTransactor[IO]("org.postgresql.Driver", dbConfig.url, dbConfig.user, dbConfig.password)
      }
      _ <- Stream.eval {
        xa.configure { hx =>
          IO { hx.setMaximumPoolSize(4) }
        }
      }
      client <- Http1Client.stream[IO]()

      cookieConfig = CookieConfig()
      mailgunConfig = MailgunConfig()

      baseUrl = "http://localhost:8080"
      tp = NowTimeProvider
      rp = SecureRandomProvider
      cs = new DefaultCookieSigner(cookieConfig.signingKey)

      authEndpoint = new AuthEndpoints[IO]().build(baseUrl, cookieConfig.cookieName, cs, xa, tp, rp, mailer)

      securedWrapper = new SecuredEndpointWrapper[IO]().build(cookieConfig.cookieName, cs, xa)(_)
      homeEndpoint = securedWrapper(new HomeEndpoints[IO]().secureService)
      gamesEndpoint = securedWrapper(new GamesEndpoints[IO](xa, client, mailgunConfig).secureService)
      teamsEndpoint = securedWrapper(new TeamsEndpoints[IO]().secureService(xa))

      code <- BlazeBuilder[IO]
        .bindLocal(port)
        .mountService(homeEndpoint)
        .mountService(gamesEndpoint, "/games")
        .mountService(teamsEndpoint, "/teams")
        .mountService(authEndpoint)
        .withExecutionContext(pool)
        .serve
    } yield code
  }
}

object CatsMailerIO {
  implicit val catsMailerIO: MailerIO[IO] = new MailerIO[IO] {
    def run(f: => Unit): IO[Unit] = IO { f }
  }
}
