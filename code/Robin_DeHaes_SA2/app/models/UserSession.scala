package models

import play.api.mvc.Session

// Object with some useful helpers for extracting user information from a session
@javax.inject.Singleton
object UserSession {
  /**
   * Get the userName of the User that is currently logged according to the Session.
   *
   * @param session the current Session
   */
  def getCurrentUserName(session: Session): String = {
    val maybeUserName = session.get(models.Global.SESSION_USERNAME_KEY)
    maybeUserName match {
      case Some(username) =>
        username
      case _ =>
        User.UnstoredUserName
    }
  }

  /**
   * Check whether we currently have a logged in User according to the Session.
   *
   * @param session the current Session
   */
  def loggedIn(session: Session): Boolean = {
    getCurrentUserName(session) != User.UnstoredUserName
  }
}
