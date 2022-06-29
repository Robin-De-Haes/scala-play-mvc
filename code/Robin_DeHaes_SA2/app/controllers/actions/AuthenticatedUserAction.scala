package controllers.actions

import controllers.routes
import play.api.mvc.Results._
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Action that requires a user to be logged in (i.e. authenticated).
 * It will reroute unauthenticated users to the home page (which is the registration page for unauthenticated users).
 */
class AuthenticatedUserAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {

    override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]) = {
        val maybeUserName = request.session.get(models.Global.SESSION_USERNAME_KEY)
        maybeUserName match {
            case None =>
                Future.successful(Redirect(routes.HomeController.index()))
            case Some(_) =>
                block(request)
        }
    }
}
