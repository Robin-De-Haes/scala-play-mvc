package controllers
import play.api.http.MimeTypes
import play.api.mvc._
import play.api.routing._

import javax.inject.{Inject, Singleton}

@Singleton
class JavaScriptRouteController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Generates Router resources to use in JavaScript.
   *
   * Retrieving sorted posts, (un)liking a post, deleting a post or adding a comment to a post
   * are currently all performed in AJAX calls and are therefore added here
   * */
  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.UserController.getSortedProfilePosts,
        routes.javascript.PostController.getSortedPosts,
        routes.javascript.PostController.toggleLike,
        routes.javascript.PostController.deletePost,
        routes.javascript.PostController.addComment
      )
    ).as(MimeTypes.JAVASCRIPT)
  }
}