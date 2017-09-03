package io.github.daddykotex.subs

import cats.data._
import cats.effect.IO
import cats.implicits._

import org.http4s._
import org.http4s.twirl._
import org.http4s.server._
import org.http4s.dsl._
import org.log4s.getLogger

object Web {
  private[this] val logger = getLogger

  val app = HttpService[IO] {
    // Note: No `Root` this is due to a limitation in the language, it sucks, but that's life
    case request @ GET -> "static" /: path =>
      StaticFile.fromResource("/static" + path.toString, Some(request)).getOrElseF(NotFound())

    case request @ POST -> Root / "signup" =>
      request.decode[UrlForm] { data =>
        data.getFirst("email") map { email =>
          SeeOther(request.uri.copy(path = "/signup"))
        } getOrElse {
          BadRequest(html.signup("Sign up"))
        }
      }

    case GET -> Root / "signup" =>
      Ok(html.signup("Sign up"))
  }
}
