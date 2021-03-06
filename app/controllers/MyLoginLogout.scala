package controllers

import jp.t2v.lab.play20.auth.{AuthConfig, LoginLogout}
import utils.session.SessionHelper

import annotation.tailrec
import util.Random
import java.security.SecureRandom
import play.api.mvc._
import models.Artist
import play.api.Play

/**
 * Created by IntelliJ IDEA.
 * User: Keyston
 * Date: 6/11/12
 * Time: 7:28 PM
 */

trait MyLoginLogout extends LoginLogout {
  self: Controller with AuthConfig =>
  override def gotoLoginSucceeded[A](userId: Id)(implicit request: Request[A]): PlainResult = {
    resolver.removeByUserId(userId)
    val sessionId = generateSessionId2(request)
    var session = resolver.store(sessionId, userId, sessionTimeoutInSeconds)
    val artist = resolveUser(userId).get.asInstanceOf[Artist]

    session = Map("id" -> String.valueOf(userId), "sessionId" -> sessionId, SessionHelper.usernameKey -> artist.name).foldLeft[Session](session) {
      _ + _
    }


    val COOKIE_NAME = Play.maybeApplication.flatMap(_.configuration.getString("session.cookieName")).getOrElse("PLAY_SESSION")
    val cookie = Session.encode(Session.serialize(session))

    val local = request.host.contains("localhost:")

    val domain = request.host.split(":").take(1).mkString

    loginSucceeded(request).withCookies(
      Cookie(COOKIE_NAME, cookie, Session.maxAge, "/", Some(domain), Session.secure, Session.httpOnly)
    )
  }


  private def generateSessionId2[A](implicit request: Request[A]): String = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890-_.!~*'()"
    val token = Stream.continually(random2.nextInt(table.size)).map(table).take(64).mkString
    if (resolver.exists(token)) generateSessionId2(request) else token
  }

  private val random2 = new Random(new SecureRandom())
}
