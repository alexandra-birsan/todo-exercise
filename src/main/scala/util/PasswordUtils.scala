package util

import java.security.MessageDigest

object PasswordUtils {

  private val digest: MessageDigest = MessageDigest.getInstance("MD5")

  def determineHash(input: String): String =
    digest.digest(input.getBytes()).map("%02x".format(_)).mkString

}
