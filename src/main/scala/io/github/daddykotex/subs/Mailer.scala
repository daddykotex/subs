package io.github.daddykotex.subs

import javax.mail.internet.InternetAddress

import courier._
import courier.miocats.CatsMailerIO._
import cats.effect.IO

class Mailer(config: EmailConfig) {
  private val mailer =
    Mailer(config.host, config.port)
      .auth(true)
      .as(config.user, config.password)
      .startTtls(true)()
  def send(e: Envelope): IO[Unit] = {
    mailer(e)
  }

  def newEnvelope(): Envelope = Envelope.from(config.user.addr)
}
