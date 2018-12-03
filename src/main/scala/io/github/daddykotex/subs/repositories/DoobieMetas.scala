package io.github.daddykotex.subs.repositories

import java.time.Instant

import doobie._
import doobie.implicits._

import tsec.passwordhashers._
import tsec.passwordhashers.imports._

object DoobieMetas {
  import java.sql.Timestamp

  implicit val ldtMeta: Meta[Instant] =
    Meta[Timestamp].xmap[Instant](
      jsqlTs => Instant.ofEpochMilli(jsqlTs.getTime),
      instant => new Timestamp(instant.toEpochMilli)
    )

  // SCrypt is a String: https://github.com/jmcardon/tsec/issues/55
  implicit val scryptMeta: Meta[PasswordHash[SCrypt]] =
    PasswordHash.is[SCrypt].substitute(Meta[String])
  //TODO
  //SCrypt.is.subst(Meta[String])
}
