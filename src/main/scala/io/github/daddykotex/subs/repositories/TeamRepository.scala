package io.github.daddykotex.subs.repositories

import doobie._
import doobie.implicits._

object TeamRepository {
  final case class Team(name: String)
  final case class TeamWithId(id: Long, name: String)

  def insertTeam(team: Team, userId: Long): ConnectionIO[TeamWithId] =
    sql"""
        INSERT INTO teams(name, user_id) VALUES(${team.name}, $userId)
      """.update.withUniqueGeneratedKeys("id", "name")

  def insertPlayerInTeam(teamId: Long, email: String): ConnectionIO[Int] =
    sql"""
        INSERT INTO team_invited_users(team_id, email)
        VALUES($teamId, $email)
      """.update.run

  def listTeamPlayers(teamId: Long): ConnectionIO[List[String]] =
    sql"""
        SELECT email FROM team_invited_users WHERE team_id = $teamId
      """.query[String].list

  def fetchTeam(teamId: Long): ConnectionIO[Option[TeamWithId]] =
    sql"""
        SELECT id, name FROM teams WHERE id = $teamId
      """.query[TeamWithId].option

  def listUserTeams(userId: Long): ConnectionIO[List[TeamWithId]] =
    sql"""
        SELECT id, name FROM teams WHERE user_id = $userId
      """.query[TeamWithId].list
}
