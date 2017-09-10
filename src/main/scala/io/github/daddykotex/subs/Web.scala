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
  private[this] val logger = getLogger

  def app(xa: Transactor[IO], tp: TimeProvider, userRepository: UserRepository): HttpService[IO] = {
    HttpService[IO] {
      case request @ GET -> "static" /: path =>
        StaticFile.fromResource("/static" + path.toString, Some(request)).getOrElseF(NotFound())

      case request @ POST -> Root / "signup" =>
        request.decode[UrlForm] { data =>
          data.getFirst("email") map { email =>
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
          } getOrElse {
            BadRequest(html.signup("Sign up"))
          }
        }

      case GET -> Root / "signup" =>
        Ok(html.signup("Sign up"))
    }
  }
}

trait TimeProvider extends Function0[Instant]
object NowTimeProvider extends TimeProvider {
  def apply(): Instant = Instant.now()
}
