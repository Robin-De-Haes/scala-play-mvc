package controllers

import controllers.actions.AuthenticatedUserAction
import models.{Global, User, UserDao}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import javax.inject.{Inject, Singleton}

@Singleton
class AuthenticationController @Inject()(
                                          cc: ControllerComponents,
                                          userDao: UserDao,
                                          authenticatedUserAction: AuthenticatedUserAction
                                        ) extends AbstractController(cc) with play.api.i18n.I18nSupport  {

    /* LOG IN */

    // form for logging in
    val loginForm: Form[User] = Form (
        mapping(
            "username" -> nonEmptyText,
            "password" -> nonEmptyText
        )(User.apply)(User.unapply)
    )

    // URL for processing a login attempt (login form submission)
    private val loginSubmitUrl = routes.AuthenticationController.processLoginAttempt()

    /**
     * Show the page with the login form
     */
    def showLoginForm = Action { implicit request =>
        Ok(views.html.authentication.userLogin(loginForm, loginSubmitUrl))
    }

    /**
     * Process a login form submission
     */
    def processLoginAttempt = Action { implicit request =>
        // Form validation/binding failed
        val errorFunction = { formWithErrors: Form[User] =>
            BadRequest(views.html.authentication.userLogin(formWithErrors, loginSubmitUrl))
        }
        // Form validation/binding succeeded
        val successFunction = { user: User =>
            userDao.findByUserName(user.username) match {
                case Some(foundUser) =>
                    Redirect(routes.PostController.showPostsOverview())
                      .flashing("INFO" -> s"Welcome ${foundUser.username}!")
                      .withSession(Global.SESSION_USERNAME_KEY -> foundUser.username)
                case None =>
                    val formWithErrors =
                        loginForm.fill(user).withGlobalError("Invalid username/password.")
                    BadRequest(views.html.authentication.userLogin(formWithErrors, loginSubmitUrl))
            }
        }
        val formValidationResult: Form[User] = loginForm.bindFromRequest
        formValidationResult.fold(
            errorFunction,
            successFunction
        )
    }

    /* LOG OUT */

    /**
     * Log out the user by discarding the session and creating a new one (which works since the authenticated user
     * is stored in the session).
     */
    def logout = authenticatedUserAction { implicit request: Request[AnyContent] =>
        Redirect(routes.AuthenticationController.showLoginForm())
          .flashing("INFO" -> "You are logged out.")
          .withNewSession
    }
}

