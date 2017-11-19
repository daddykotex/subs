package io.github.daddykotex.subs.utils

import java.time.Instant
import java.security.SecureRandom

import org.reactormonk.{CryptoBits, PrivateKey}

trait TimeProvider extends Function0[Instant]
object NowTimeProvider extends TimeProvider {
  def apply(): Instant = Instant.now()
}

trait RandomProvider extends Function1[Int, String]
object SecureRandomProvider extends RandomProvider {
  private val alphaNumeric = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
  private val random = new SecureRandom()
  private def gen(length: Int) = {
    val buffer = Array.tabulate(length) { _ =>
      alphaNumeric(random.nextInt(alphaNumeric.length))
    }
    new String(buffer)
  }

  def apply(length: Int): String = gen(length)
}

trait CookieSigner[T] {
  def sign(message: String, tp: TimeProvider): T
  def validate(encrypted: String): Either[String, T]
}
class DefaultCookieSigner(key: String) extends CookieSigner[String] {
  val pk = PrivateKey(scala.io.Codec.toUTF8(key))
  val crypto = CryptoBits(pk)
  override def sign(message: String, tp: TimeProvider): String =
    crypto.signToken(message, tp().toEpochMilli().toString)

  override def validate(encrypted: String): Either[String, String] =
    crypto.validateSignedToken(encrypted).toRight("Cookie invalid")

}
