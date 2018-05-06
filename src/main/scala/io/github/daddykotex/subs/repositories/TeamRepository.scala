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

  def fetchTeam(teamId: Long): ConnectionIO[Option[TeamWithId]] =
    sql"""
        SELECT id, name FROM teams WHERE id = $teamId
      """.query[TeamWithId].option

  def listUserTeams(userId: Long): ConnectionIO[List[TeamWithId]] =
    sql"""
        SELECT id, name FROM teams WHERE user_id = $userId
      """.query[TeamWithId].list
}
