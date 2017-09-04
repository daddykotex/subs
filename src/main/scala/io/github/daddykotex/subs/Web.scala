package io.github.daddykotex.subs

import java.time.Instant

import cats._
import cats.data._
import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.free.connection
import doobie.implicits._
import org.http4s._
import org.http4s.twirl._
import org.http4s.dsl._
import org.log4s.getLogger

object Web {
  private[this] val logger = getLogger

  import TimeProvider.TimeProvider

  def app(xa: Transactor[IO], tp: TimeProvider, userRepository: UserRepository): HttpService[IO] = {
    HttpService[IO] {
      case request @ GET -> "static" /: path =>
        StaticFile.fromResource("/static" + path.toString, Some(request)).getOrElseF(NotFound())

      case request @ POST -> Root / "signup" =>
        request.decode[UrlForm] { data =>
          data.getFirst("email") map { email =>
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

object TimeProvider {
  type TimeProvider = Unit => Instant
  def now(): TimeProvider = _ => Instant.now()
}
