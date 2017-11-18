package io.github.daddykotex.subs

import java.time.Instant
import java.time.temporal.ChronoUnit

import doobie._
import doobie.implicits._

case class VerifiedUser(email: String, password: String, name: String, roles: String)
case class UnverifiedUser(email: String, signupDatetime: Instant, token: String) {
  def hasNotExpired(time: Instant): Boolean =
    ChronoUnit.DAYS.between(time, this.signupDatetime) < 7
}

sealed trait UserRepository {
  def isEmailUsed(email: String): ConnectionIO[Boolean]
  def insertUnverifiedUser(email: String, ts: Instant, token: String): ConnectionIO[Int]
  def removeUnverifiedUser(email: String): ConnectionIO[Int]
  def fetchUnverifiedUser(token: String): ConnectionIO[Option[UnverifiedUser]]
  def insertVerififedUser(email: String, password: String, name: String, roles: String): ConnectionIO[Int]
  def fetchVerifiedUser(email: String): ConnectionIO[Option[VerifiedUser]]
}

object Repositories {
  import DoobieMetas._
  def userRepo(): UserRepository = new UserRepository {
    override def isEmailUsed(email: String): ConnectionIO[Boolean] =
      sql"""
        SELECT COUNT(email) FROM unverified_users uu WHERE email = $email
        UNION
        SELECT COUNT(email) FROM verified_users WHERE email = $email
      """.query[Int].list.map { counts =>
        counts.exists(_ > 0)
      }

    override def insertUnverifiedUser(email: String, signupDatetime: Instant, token: String): ConnectionIO[Int] =
      sql"""
        INSERT INTO unverified_users(email, signup_datetime, token) VALUES($email, $signupDatetime, $token)
      """.update.run

    override def removeUnverifiedUser(email: String): ConnectionIO[Int] =
      sql"""
        DELETE FROM unverified_users WHERE email = $email
      """.update.run

    override def fetchUnverifiedUser(token: String): ConnectionIO[Option[UnverifiedUser]] =
      sql"""
        SELECT email, signup_datetime, token FROM unverified_users WHERE token = $token
      """.query[UnverifiedUser].option

    override def insertVerififedUser(email: String, password: String, name: String, roles: String): ConnectionIO[Int] =
      sql"""
        INSERT INTO verified_users(email, password, name, roles) VALUES($email, $password, $name, $roles)
      """.update.run

    override def fetchVerifiedUser(email: String): ConnectionIO[Option[VerifiedUser]] =
      sql"""
        SELECT email, password, name, roles FROM verified_users WHERE email = $email
      """.query[VerifiedUser].option
  }
}

private object DoobieMetas {
  import java.sql.Timestamp

  implicit val ldtMeta: Meta[Instant] =
    Meta[Timestamp].xmap[Instant](
      jsqlTs => Instant.ofEpochMilli(jsqlTs.getTime),
      instant => new Timestamp(instant.toEpochMilli)
    )
}
