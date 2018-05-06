package io.github.daddykotex.subs.web

import io.github.daddykotex.subs.repositories.UserRepository

import cats.effect._

import org.http4s._
import org.http4s.twirl._
import org.http4s.dsl.Http4sDsl

class GamesEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  import UserRepository.VerifiedUser

  val secureService: AuthedService[VerifiedUser, F] =
    AuthedService[VerifiedUser, F] {
      case GET -> Root as user =>
        Ok(html.home("Your games", Some(user.email)))
    }
}
