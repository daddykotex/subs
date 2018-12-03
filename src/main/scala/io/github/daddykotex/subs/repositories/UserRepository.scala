package io.github.daddykotex.subs.repositories

import java.time.Instant
import java.time.temporal.ChronoUnit

import doobie._
import doobie.implicits._

import tsec.passwordhashers._
import tsec.passwordhashers.imports._

object UserRepository {
  import DoobieMetas._

  case class VerifiedUser(id: Long, email: String, password: PasswordHash[SCrypt], name: String, roles: String)
  case class UnverifiedUser(email: String, signupDatetime: Instant, token: String) {
    def hasNotExpired(time: Instant): Boolean =
      ChronoUnit.DAYS.between(time, this.signupDatetime) < 7
  }

  def isEmailUsed(email: String): ConnectionIO[Boolean] =
    sql"""
        SELECT COUNT(email) FROM unverified_users uu WHERE email = $email
        UNION
        SELECT COUNT(email) FROM verified_users WHERE email = $email
      """.query[Int].list.map { counts =>
      counts.exists(_ > 0)
    }

  def insertUnverifiedUser(email: String, signupDatetime: Instant, token: String): ConnectionIO[Int] =
    sql"""
        INSERT INTO unverified_users(email, signup_datetime, token) VALUES($email, $signupDatetime, $token)
      """.update.run

  def removeUnverifiedUser(email: String): ConnectionIO[Int] =
    sql"""
        DELETE FROM unverified_users WHERE email = $email
      """.update.run

  def fetchUnverifiedUser(token: String): ConnectionIO[Option[UnverifiedUser]] =
    sql"""
        SELECT email, signup_datetime, token FROM unverified_users WHERE token = $token
      """.query[UnverifiedUser].option

  def insertVerififedUser(email: String,
                          password: PasswordHash[SCrypt],
                          name: String,
                          roles: String): ConnectionIO[Int] =
    sql"""
        INSERT INTO verified_users(email, password, name, roles) VALUES($email, $password, $name, $roles)
      """.update.run

  def fetchVerifiedUser(email: String): ConnectionIO[Option[VerifiedUser]] =
    sql"""
        SELECT id, email, password, name, roles FROM verified_users WHERE email = $email
      """.query[VerifiedUser].option

  def fetchVerifiedUser(id: Long): ConnectionIO[Option[VerifiedUser]] =
    sql"""
        SELECT id, email, password, name, roles FROM verified_users WHERE id = $id
      """.query[VerifiedUser].option
}
