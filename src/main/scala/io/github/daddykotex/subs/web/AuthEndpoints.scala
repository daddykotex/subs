package io.github.daddykotex.subs.web

import com.github.daddykotex.courier.{addr, Multipart}

import io.github.daddykotex.subs.{Mailer => SMailer}
import io.github.daddykotex.subs.utils._
import io.github.daddykotex.subs.repositories.UserRepository

import cats.effect._, cats.implicits._
import doobie._
import doobie.free.connection
import doobie.implicits._
import org.http4s._
import org.http4s.twirl._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.HttpService

import org.log4s.getLogger

class AuthEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  import AuthForms._

  private[this] val log = getLogger

  def build(
      baseUrl: String,
      cookieName: String,
      cs: CookieSigner[String],
      xa: Transactor[F],
      tp: TimeProvider,
      rp: RandomProvider,
      userRepository: UserRepository,
      mailer: SMailer[F]
  ): HttpService[F] = HttpService[F] {
    case request @ GET -> "static" /: path =>
      StaticFile.fromResource("/static" + path.toString, Some(request)).getOrElseF(NotFound())

    case request @ POST -> Root / "signup" =>
      request.decode[SignUpForm] { sForm =>
        val email = sForm.email
        val token = rp(50)
        val emailContent =
          mailer
            .newEnvelope()
            .to(email.addr)
            .subject("Welcome to Subs")
            .content(Multipart().html(emails.html.verify_email(s"$baseUrl/verify?token=$token").body))
        log.info(s"Processing signup for $email")
        val dbOp = for {
          exists <- userRepository.isEmailUsed(email)
          inserted <- if (!exists) {
            userRepository.insertUnverifiedUser(email, tp(), token).map(_ => true)
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

    case request @ POST -> Root / "complete" =>
      request.decode[CompleteForm] { cForm =>
        (for {
          maybeUser <- userRepository.fetchUnverifiedUser(cForm.token)
          res <- maybeUser
            .map { uu =>
              for {
                _ <- userRepository
                  .insertVerififedUser(uu.email, cForm.password, cForm.name, "users") //TODO hash password
                _ <- userRepository.removeUnverifiedUser(uu.email)
              } yield SeeOther(Location(request.uri.copy(path = "/signin?success")))
            }
            .getOrElse { connection.delay(SeeOther(Location(request.uri.copy(path = "/signup?error=invalidtoken")))) }
        } yield res).transact(xa).flatten
      }

    case request @ GET -> Root / "verify" :? TokenQueryParamMatcher(token) =>
      userRepository.fetchUnverifiedUser(token).transact(xa) flatMap {
        case Some(uu) if uu.hasNotExpired(tp()) =>
          Ok(html.complete("Complete your sign up", token))
        case None =>
          SeeOther(Location(request.uri.copy(path = "/signup?error=invalidtoken")))
      }

    case GET -> Root / "signup" =>
      Ok(html.signup("Sign up"))

    case request @ POST -> Root / "signin" =>
      request.decode[SignInForm] { signinForm =>
        (userRepository.fetchVerifiedUser(signinForm.email) map { maybeUser =>
          maybeUser
            .filter(_.password == signinForm.password)
            .map { user =>
              val cookieValue = cs.sign(user.id.toString, tp)
              SeeOther(Location(request.uri.copy(path = "/")))
                .map(_.addCookie(Cookie(cookieName, cookieValue)))
            }
            .getOrElse {
              SeeOther(Location(request.uri.copy(path = "/signin?error=invalidusernamepassword")))
            }
        }).transact(xa).flatten
      }

    case GET -> Root / "signin" =>
      Ok(html.signin("Sign in"))
  }
}

private object TokenQueryParamMatcher extends QueryParamDecoderMatcher[String]("token")

object AuthForms {
  private val invalidForm = InvalidMessageBodyFailure("Form submission is invalid")
  case class SignUpForm(email: String)
  object SignUpForm {
    implicit def signupFormDecoder[F[_]: Effect](
        implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, SignUpForm] =
      original.flatMapR[SignUpForm] {
        _.getFirst("email") match {
          case Some(email) => DecodeResult.success(SignUpForm(email))
          case _ => DecodeResult.failure(invalidForm)
        }
      }
  }

  case class CompleteForm(token: String, name: String, password: String)
  object CompleteForm {
    implicit def completeFormDecoder[F[_]: Effect](
        implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, CompleteForm] =
      original.flatMapR[CompleteForm] { uf =>
        (
          for {
            token <- uf.getFirst("token")
            name <- uf.getFirst("name")
            password <- uf.getFirst("password")
            confirmPassword <- uf.getFirst("password_confirm")
            if password == confirmPassword
          } yield CompleteForm(token, name, password)
        ) match {
          case Some(x) => DecodeResult.success(x)
          case None => DecodeResult.failure(invalidForm)
        }
      }
  }

  case class SignInForm(email: String, password: String)
  object SignInForm {
    implicit def signInFormDecoder[F[_]: Effect](
        implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, SignInForm] =
      original.flatMapR[SignInForm] { uf =>
        (
          for {
            email <- uf.getFirst("email")
            password <- uf.getFirst("password")
          } yield SignInForm(email, password)
        ) match {
          case Some(x) => DecodeResult.success(x)
          case None => DecodeResult.failure(invalidForm)
        }
      }
  }
}
