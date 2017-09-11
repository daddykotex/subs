package io.github.daddykotex.subs

import java.time.Instant

import cats.effect.IO
import doobie._
import doobie.free.connection
import doobie.implicits._
import org.http4s._
import org.http4s.twirl._
import org.http4s.dsl._
import org.log4s.getLogger

object Web {
  import Forms._

  private[this] val logger = getLogger

  def app(xa: Transactor[IO], tp: TimeProvider, userRepository: UserRepository): HttpService[IO] = {
    HttpService {
      case request @ GET -> "static" /: path =>
        StaticFile.fromResource("/static" + path.toString, Some(request)).getOrElseF(NotFound())

      case request @ POST -> Root / "signup" =>
        request.decode[SignUpForm] { sForm =>
          val email = sForm.email
          logger.info(s"Processing signup for $email")
          val dbOp = for {
            exists <- userRepository.isEmailUsed(email)
            _ <- if (!exists) {
              userRepository.insertUnverifiedUser(email, tp())
            } else {
              connection.delay(0)
            }
          } yield exists
          dbOp.transact(xa).flatMap(_ => SeeOther(request.uri.copy(path = "/signup")))
        }

      case GET -> Root / "signup" =>
        Ok(html.signup("Sign up"))
    }
  }
}

object Forms {
  case class SignUpForm(email: String)
  object SignUpForm {
    implicit def signupFormDecoder(implicit original: EntityDecoder[IO, UrlForm]): EntityDecoder[IO, SignUpForm] =
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
