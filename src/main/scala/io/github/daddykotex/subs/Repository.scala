package io.github.daddykotex.subs

import java.time.Instant

import doobie._
import doobie.implicits._
import doobie.free.connection

sealed trait UserRepository {
  def isEmailUsed(email: String): ConnectionIO[Boolean]
  def insertUnverifiedUser(email: String, ts: Instant): ConnectionIO[Int]
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

    override def insertUnverifiedUser(email: String, ts: Instant): ConnectionIO[Int] =
      sql"""
        INSERT INTO unverified_users(email, signup_datetime) VALUES($email, $ts)
      """.update.run
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
