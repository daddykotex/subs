package io.github.daddykotex.subs.web

import io.github.daddykotex.subs.utils._
import io.github.daddykotex.subs.repositories.UserRepository

import cats.effect._
import cats.data._
import cats.implicits._

import doobie._
import doobie.implicits._

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.HttpService
import org.http4s.server._

class SecuredEndpointWrapper[F[_]: Effect] extends Http4sDsl[F] {
  import UserRepository.VerifiedUser

  def build(
      cookieName: String,
      cs: CookieSigner[String],
      xa: Transactor[F]
  )(route: AuthedService[VerifiedUser, F]): HttpService[F] = {
    val findUser: Kleisli[F, Long, Either[String, VerifiedUser]] = Kleisli { id =>
      UserRepository
        .fetchVerifiedUser(id)
        .map { _.toRight(s"The user with id: '$id' was not found") }
        .transact(xa)
    }

    val authUser: Kleisli[F, Request[F], Either[String, VerifiedUser]] =
      Kleisli { req =>
        val message: Either[String, Long] = for {
          cookies <- headers.Cookie.from(req.headers).toRight("Cookie parsing error")
          cookie <- cookies.values.toList.find(_.name == cookieName).toRight("Couldn't find the authentication cookie")
          token <- cs.validate(cookie.content)
          message <- Either.catchOnly[NumberFormatException](token.toLong).leftMap(_.toString)
        } yield message

        message match {
          case Right(userId) =>
            findUser.run(userId)
          case Left(x) =>
            (Left(x): Either[String, VerifiedUser]).pure[F]
        }
      }

    val onFailure: AuthedService[String, F] = Kleisli(
      authReq => OptionT.liftF(SeeOther(Location(authReq.req.uri.copy(path = "/signin")))))

    val middleware: AuthMiddleware[F, VerifiedUser] = AuthMiddleware(authUser, onFailure)

    middleware(route)
  }
}
