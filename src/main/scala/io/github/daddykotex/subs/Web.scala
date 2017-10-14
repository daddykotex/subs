package io.github.daddykotex.subs

import java.time.Instant

import com.github.daddykotex.courier.addr
import com.github.daddykotex.courier.{Multipart}

import io.github.daddykotex.subs.{Mailer => SMailer}

import cats.effect.Sync
import cats._
import cats.data._
import cats.implicits._
import doobie._
import doobie.free.connection
import doobie.implicits._
import org.http4s._
import org.http4s.twirl._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.headers.Location
import org.http4s.HttpService

import org.log4s.getLogger

class Endpoints[F[_]: Sync] extends Http4sDsl[F] {
  import Forms._

  private[this] val logger = getLogger

  def app(
      baseUrl: String, //TODO using something else than a String
      xa: Transactor[F],
      tp: TimeProvider,
      userRepository: UserRepository,
      mailer: SMailer[F]
  ): HttpService[F] = {
    HttpService {
      case request @ GET -> "static" /: path =>
        StaticFile.fromResource("/static" + path.toString, Some(request)).getOrElseF(NotFound())

      case request @ POST -> Root / "signup" =>
        request.decode[SignUpForm] { sForm =>
          val email = sForm.email
          val emailContent =
            mailer
              .newEnvelope()
              .to(email.addr)
              .subject("Welcome to Subs")
              .content(Multipart().html(emails.html.verify_email(s"$baseUrl/verify").body))
          logger.info(s"Processing signup for $email")
          val dbOp = for {
            exists <- userRepository.isEmailUsed(email)
            inserted <- if (!exists) {
              userRepository.insertUnverifiedUser(email, tp()).map(_ => true)
            } else {
              connection.delay(false)
            }
          } yield inserted
          for {
            inserted <- dbOp.transact(xa)
            _ <- if (inserted) mailer.send(emailContent) else ().pure[F]
            resp <- SeeOther(Location(request.uri.copy(path = "/signup")))
          } yield resp
        }

      case GET -> Root / "signup" =>
        Ok(html.signup("Sign up"))
    }
  }
}

object Forms {
  case class SignUpForm(email: String)
  object SignUpForm {
    implicit def signupFormDecoder[F[_]: Sync](
        implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, SignUpForm] =
      original.flatMapR[SignUpForm] {
        _.getFirst("email") match {
          case Some(email) => DecodeResult.success(SignUpForm(email))
          case _ => DecodeResult.failure(InvalidMessageBodyFailure("'email' is required"))
        }
      }
  }
}

trait TimeProvider extends Function0[Instant]
object NowTimeProvider extends TimeProvider {
  def apply(): Instant = Instant.now()
}
