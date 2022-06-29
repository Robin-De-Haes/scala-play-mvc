package controllers

import models.{User, UserSession}
import play.api.mvc._

import javax.inject._

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Render the home page. This method will be called when the application receives a `GET` request with
   * a path of '/' or '/index'.
   *
   * It redirects to the login page for unauthenticated users and to the 'exploration' page for authenticated users.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    val currentUser = UserSession.getCurrentUserName(request.session)
    if (currentUser == User.UnstoredUserName) {
      Redirect(routes.AuthenticationController.showLoginForm())
        .flashing("INFO" -> "Welcome to the Social Student! Please login to be able to explore posts.")
    }
    else {
      Redirect(routes.PostController.showPostsOverview())
        .flashing("INFO" -> s"Welcome $currentUser! Explore and enjoy.")
    }
  }
}
