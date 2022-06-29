package controllers

import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import javax.inject.Inject

class UserController @Inject()(
                                cc: MessagesControllerComponents,
                                userDao: UserDao,
                                postDao: PostDao,
                              ) extends AbstractController(cc)  with play.api.i18n.I18nSupport {

    /* REGISTRATION (I.E. USER CREATION) */

    // Registration form
    val registrationForm: Form[User] = Form (
        mapping(
            "username" -> nonEmptyText
              .verifying("Username should be longer than 2 characters.",  input => lengthIsGreaterThanNCharacters(input, 2))
              .verifying("Username should be shorter than 20 characters", input => lengthIsLessThanNCharacters(input, 20))
              .verifying("A user with that username already exists.", input => isUniqueUserName(input)),
            "password" -> nonEmptyText
              .verifying("Password should be longer than 5 characters.",  input => lengthIsGreaterThanNCharacters(input, 5))
              .verifying("Password should contain a lower case character.",  input => containsLowerCase(input))
              .verifying("Password should contain an upper case character.",  input => containsUpperCase(input))
              .verifying("Password should contain a number.",  input => containsNumber(input))
              .verifying("Password should be less than 30 characters.", input => lengthIsLessThanNCharacters(input, 30))
        )(User.apply)(User.unapply)
    )

    // URL for processing a registration attempt (registration form submission)
    private val registrationSubmitUrl = routes.UserController.processRegistrationAttempt()

    /**
     * Show the page with the registration form
     */
    def showRegistrationForm = Action { implicit request =>
        Ok(views.html.users.userRegistration(registrationForm, registrationSubmitUrl))
    }

    /**
     * Process the registration form submission
     */
    def processRegistrationAttempt = Action { implicit request =>
        val formValidationResult: Form[User] = registrationForm.bindFromRequest
        formValidationResult.fold(
            // Form validation/binding failed
            hasErrors = { formWithErrors: Form[User] =>
                BadRequest(views.html.users.userRegistration(formWithErrors, registrationSubmitUrl))
            },
            // Form validation/binding succeeded
            success = { user: User =>
                // store the User
                if (!userDao.addUser(user)) {
                    InternalServerError("Something went wrong while registering you. Please try again!")
                }

                val message = "You have been successfully registered! Please login."
                Redirect(routes.AuthenticationController.showLoginForm())
                  .flashing("SUCCESS" -> message)
            }
        )
    }

    /**
     * Helper to verify if a given input is longer than the specified minLength
     */
    private def lengthIsGreaterThanNCharacters(input: String, minLength: Int): Boolean = {
        input.length > minLength
    }

    /**
     * Helper to verify if a given input is shorter than the specified maxLength
     */
    private def lengthIsLessThanNCharacters(input: String, maxLength: Int): Boolean = {
        input.length < maxLength
    }

    /**
     * Helper to verify if a given input contains a lowercase character
     */
    private def containsLowerCase(input: String): Boolean = {
        input.exists(_.isLower)
    }

    /**
     * Helper to verify if a given input contains an uppercase character
     */
    private def containsUpperCase(input: String): Boolean = {
        input.exists(_.isUpper)
    }

    /**
     * Helper to verify if a given input contains a number
     */
    private def containsNumber(input: String): Boolean = {
        input.exists(_.isDigit)
    }

    /**
     * Helper to verify if a given username is unique (i.e. doesn't exist yet)
     */
    private def isUniqueUserName(username: String): Boolean = {
        !userDao.isExistingUser(username)
    }

    /* USER PROFILE */

    // The default number of comments to show per post on a profile page
    def profileMaxComments = 2

    /**
     * Shows a user's profile page.
     *
     * @param profileUserName the username of the user to show the profile of
     */
    def showProfile(profileUserName: String) =
        getSortedProfilePosts(profileUserName, PostDao.SortingField_Date, postsOnly = false)

    /**
     * Get the sorted posts of a specific user
     *
     * In practice, it is currently more a Post-related action since it only performs a filtering of posts,
     * but nevertheless it seemed wise to put as a User oriented action (since it can then easily be modified to
     * perform more user related behavior)
     *
     * @param profileUserName the username of the user to get the posts from
     * @param sortOn Post field to sort on. Should be one of {@link PostDao.SortingField_Date} or {@link PostDao.SortingField_Date}
     * @param postsOnly Boolean specifying whether a list of HTML formatted posts should be returned (if true)
     * or a full HTML page (if false). Calls with postsOnly = true are intended to be used for AJAX calls.
     */
    def getSortedProfilePosts(profileUserName : String, sortOn: String, postsOnly: Boolean) = Action { implicit request =>
        userDao.findByUserName(profileUserName).map{ profileUser =>
            val currentUserName = UserSession.getCurrentUserName(request.session)

            // if the user is not logged in, don't show any posts
            val posts = if (currentUserName == User.UnstoredUserName) {
                List()
            }
            else {
                // get the visible Posts of the User whose profile you're visiting
                postDao.getVisiblePosts(currentUserName, sortOn, _.posterName.equalsIgnoreCase(profileUserName))
            }

            if (postsOnly)
                Ok(views.html.posts.postsList(posts, profileMaxComments)) // AJAX update
            else
                Ok(views.html.users.userProfile(profileUser, posts, profileMaxComments)) // full HTML page
        }.getOrElse(NotFound)
    }
}
