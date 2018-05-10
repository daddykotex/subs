package io.github.daddykotex.subs.web

import io.github.daddykotex.subs.repositories._
import io.github.daddykotex.subs.repositories.TeamRepository._
import io.github.daddykotex.subs.utils._

import cats.effect._, cats.implicits._
import doobie._
import doobie.free.connection
import doobie.implicits._

import org.http4s._
import org.http4s.twirl._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location

class TeamsEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  import UserRepository.VerifiedUser
  import TeamsForm._
  import TeamRepository._

  def secureService(xa: Transactor[F]): AuthedService[VerifiedUser, F] =
    AuthedService[VerifiedUser, F] {
      case GET -> Root as user =>
        TeamRepository
          .listUserTeams(user.id)
          .transact(xa)
          .flatMap { teams =>
            Ok(html.teams("Your teams", teams, Some(user.email)))
          }

      case authed @ POST -> Root / "create" as user =>
        authed.req.decode[CreateTeamForm] { teamForm =>
          TeamRepository
            .insertTeam(Team(teamForm.name), user.id)
            .transact(xa)
            .flatMap(inserted => SeeOther(Location(authed.req.uri.copy(path = s"/teams/${inserted.id}"))))
        }
      case GET -> Root / LongVar(teamId) / "players" / "add" as user =>
        TeamRepository
          .fetchTeam(teamId)
          .transact(xa)
          .flatMap {
            case Some(team) =>
              Ok(html.add_player_team(team, Some(user.email)))
            case None =>
              NotFound()
          }
      case authed @ POST -> Root / LongVar(teamId) / "players" / "add" as _ =>
        TeamRepository
          .fetchTeam(teamId)
          .transact(xa)
          .flatMap {
            case Some(team) =>
              authed.req.decode[AddPlayerToTeam] { form =>
                TeamRepository
                  .insertPlayerInTeam(team.id, form.email)
                  .transact(xa)
                  .flatMap(_ => SeeOther(Location(authed.req.uri.copy(path = s"/teams/${team.id}"))))
              }
            case None =>
              NotFound()
          }
      case GET -> Root / LongVar(teamId) as user =>
        for {
          maybeTeam <- TeamRepository.fetchTeam(teamId).transact(xa)
          players <- maybeTeam
            .map(t => TeamRepository.listTeamPlayers(t.id).transact(xa))
            .getOrElse(List.empty[String].pure[F])
          res <- maybeTeam
            .map(t => Ok(html.one_team(t, players, Some(user.email))))
            .getOrElse(NotFound())
        } yield res

      case GET -> Root / "create" as user =>
        Ok(html.create_team("New team", Some(user.email)))
    }
}

object TeamsForm {
  final case class CreateTeamForm(name: String) extends AnyVal
  final case class AddPlayerToTeam(email: String) extends AnyVal

  implicit def addTeamPlayerFormDecoder[F[_]: Effect](
      implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, AddPlayerToTeam] =
    original.flatMapR[AddPlayerToTeam] { uf =>
      (
        uf.getFirst("email").map(AddPlayerToTeam.apply)
      ) match {
        case Some(x) => DecodeResult.success(x)
        case None => DecodeResult.failure(FormUtils.invalidForm())
      }
    }

  implicit def createTeamFormDecoder[F[_]: Effect](
      implicit original: EntityDecoder[F, UrlForm]): EntityDecoder[F, CreateTeamForm] =
    original.flatMapR[CreateTeamForm] { uf =>
      (
        uf.getFirst("name").map(CreateTeamForm.apply)
      ) match {
        case Some(x) => DecodeResult.success(x)
        case None => DecodeResult.failure(FormUtils.invalidForm())
      }
    }
}
