package io.github.daddykotex.subs

import javax.mail.internet.InternetAddress

import com.github.daddykotex.courier.{Envelope, Mailer => CMailer, MailerIO}
import com.github.daddykotex.courier.addr
import cats.implicits._
import cats.effect.Sync

class Mailer[F[_]: Sync: MailerIO](config: EmailConfig) {
  private val mailer =
    CMailer(config.host, config.port)
      .auth(true)
      .as(config.user, config.password)
      .startTtls(true)()
  def send(e: Envelope): F[Unit] = mailer(e)

  def newEnvelope(): Envelope = Envelope.from(config.user.addr)
}
