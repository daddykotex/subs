package io.github.daddykotex.subs.web

import java.time.Instant

import io.github.daddykotex.subs.MailgunConfig
import io.github.daddykotex.subs.repositories._
import io.github.daddykotex.subs.repositories.GameRepository.{Game, GameWithId}
import io.github.daddykotex.subs.repositories.TeamRepository.TeamWithId
import io.github.daddykotex.subs.utils._

import cats._, cats.effect._, cats.implicits._
import cats.data.NonEmptyList
import doobie._
import doobie.free.connection
import doobie.implicits._

import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.twirl._
import org.http4s.dsl.Http4sDsl
import org.http4s.MediaType._

import scala.util.Try

class GamesEndpoints[F[_]: Effect: Monad](xa: Transactor[F], client: Client[F], mailgunConfig: MailgunConfig)
    extends Http4sDsl[F]
    with Http4sClientDsl[F] {
  import UserRepository.VerifiedUser
  import GamesForm._

  private def gameInfo(gameId: Long): ConnectionIO[(Option[GameWithId], Option[TeamWithId], List[String])] =
    for {
      maybeGameAndId <- GameRepository.fetchGame(gameId)
      maybeGame = maybeGameAndId.map(_._1)
      maybeTeamId = maybeGameAndId.map(_._2)

      maybeTeam <- maybeTeamId
        .map(tid => TeamRepository.fetchTeam(tid))
        .getOrElse(connection.delay(Option.empty[TeamWithId]))

      players <- maybeTeamId
        .map(tid => TeamRepository.listTeamPlayers(tid))
        .getOrElse(connection.delay(List.empty[String]))
    } yield (maybeGame, maybeTeam, players)

  def secureService(): AuthedService[VerifiedUser, F] =
    AuthedService[VerifiedUser, F] {
      case GET -> Root as user =>
        GameRepository
          .listUserGames(user.id)
          .transact(xa)
          .flatMap { games =>
            Ok(html.games("Your games", games, Some(user.email)))
          }

      case authed @ POST -> Root / "create" as user =>
        authed.req.decode[CreateGameForm] { form =>
          GameRepository
            .insertUserGame(Game(form.location, form.opponent, form.startDate), user.id, form.teamId)
            .transact(xa)
            .flatMap { games =>
              SeeOther(Location(authed.req.uri.copy(path = "/games")))
            }
        }

      case authed @ POST -> Root / LongVar(gameId) / "invite" as _ =>
        authed.req.decode[SendInviteForm] { form =>
          gameInfo(gameId)
            .transact(xa)
            .flatMap {
              case (Some(game), _, players) =>
                val emailList = players.intersect(form.whitelist.toList)
                val emailsIO = emailList.traverse(e => sendMail(game, e))
                emailsIO.flatMap { responses =>
                  println(responses)
                  SeeOther(Location(authed.req.uri.copy(path = s"/games/${game.id}/invite")))
                }
              case _ =>
                NotFound()
            }
        }

      case GET -> Root / LongVar(gameId) / "invite" as user =>
        gameInfo(gameId)
          .transact(xa)
          .flatMap {
            case (Some(game), Some(team), players) =>
              Ok(html.game_invite(game, team, players, Some(user.email)))
            case _ =>
              NotFound()
          }

      case GET -> Root / "create" as user =>
        TeamRepository
          .listUserTeams(user.id)
          .transact(xa)
          .flatMap { teams =>
            Ok(html.create_game("New game", teams, Some(user.email)))
          }
    }

  private def sendMail(game: GameWithId, email: String): F[String] = {
    val lol = s"$mailgunConfig $client $game $email"
    println(lol)
    lol.pure[F]
    // client.expect[String](
    //   Method.POST(
    //     mailgunConfig.baseUri / "messages",
    //     UrlForm(
    //       "from" -> "Mailgun Sandbox <postmaster@sandbox1df3293b57a84ede8d81ef4a7d22d7c5.mailgun.org>",
    //       "to" -> "David Francoeur <dfrancoeur04@gmail.com>",
    //       "subject" -> "Hello David Francoeur",
    //       "html" -> "<h1>hello</h1>",
    //       "text" -> "Congratulations David Francoeur, you just sent an email with Mailgun!  You are truly awesome!"
    //     ),
    //     Authorization(BasicCredentials("api", "key-5fb8a100b3fd8a6c57b9e36d1edc0c41"))
    //   ))
  }
}

object GamesForm {
  final case class CreateGameForm(teamId: Long, location: String, opponent: String, startDate: Instant)

  implicit def addTeamPlayerFormDecoder[F[_]: Effect](
      implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, CreateGameForm] =
    original.flatMapR[CreateGameForm] { uf =>
      (for {
        teamId <- uf.getFirst("teamid").flatMap(st => Try(st.toLong).toOption)
        date <- uf.getFirst("date")
        time <- uf.getFirst("time")
        startDate <- Try(Instant.parse(s"${date}T${time}:00Z")).toOption
        opponent <- uf.getFirst("opponent")
        location <- uf.getFirst("location")
      } yield CreateGameForm(teamId, location, opponent, startDate)) match {
        case Some(x) => DecodeResult.success(x)
        case None => DecodeResult.failure(FormUtils.invalidForm())
      }
    }

  final case class SendInviteForm(whitelist: NonEmptyList[String])

  implicit def sendInviteFormFormDecoder[F[_]: Effect](
      implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, SendInviteForm] =
    original.flatMapR[SendInviteForm] { uf =>
      uf.get("invitation").toList.toNel match {
        case Some(nel) => DecodeResult.success(SendInviteForm(nel))
        case None => DecodeResult.failure(FormUtils.invalidForm("You must at least select one recipient."))
      }
    }
}
