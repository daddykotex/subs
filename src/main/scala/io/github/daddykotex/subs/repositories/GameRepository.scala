package io.github.daddykotex.subs.repositories

import java.time.Instant

import doobie._
import doobie.implicits._

object GameRepository {
  import DoobieMetas._

  final case class Game(location: String, opponent: String, startDate: Instant)
  final case class GameWithId(id: Long, location: String, opponent: String, startDate: Instant)

  def listUserGames(userId: Long): ConnectionIO[List[GameWithId]] =
    sql"""
        SELECT id, location, opponent, start_date
        FROM games WHERE user_id = $userId
      """.query[GameWithId].list

  def fetchGame(gameId: Long): ConnectionIO[Option[(GameWithId, Long)]] =
    sql"""
        SELECT id, location, opponent, start_date, team_id FROM games WHERE id = $gameId
      """.query[(GameWithId, Long)].option

  def fetchGameTeamId(gameId: Long): ConnectionIO[Option[Long]] =
    sql"""
        SELECT team_id FROM games WHERE id = $gameId
      """.query[Long].option

  def insertUserGame(game: Game, userId: Long, teamId: Long): ConnectionIO[GameWithId] =
    sql"""
        INSERT INTO games(user_id, team_id, location, opponent, start_date)
        VALUES($userId, $teamId, ${game.location}, ${game.opponent}, ${game.startDate})
      """.update.withUniqueGeneratedKeys("id", "location", "opponent", "start_date")
}
