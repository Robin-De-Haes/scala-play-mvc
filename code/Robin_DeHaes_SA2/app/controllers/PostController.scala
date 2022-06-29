package controllers

import controllers.actions.AuthenticatedUserAction
import models._
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.mvc._

import java.nio.file.{Files, Paths}
import javax.inject.Inject

class PostController @Inject()(cc: ControllerComponents,
                               authenticatedUserAction: AuthenticatedUserAction,
                               userDao: UserDao,
                               postDao: PostDao)
  extends AbstractController(cc) with play.api.i18n.I18nSupport {

  /* POST DETAILS */

  /**
   * Show a detailed view of a given post
   *
   * @param postID the ID of the post to show
   */
  def showPost(postID: Long) = authenticatedUserAction { implicit request =>
    postDao.findByID(postID).map { post =>
      // check if the currently logged in User is allowed to view the Post,
      // we always have a logged in User since this is an authenticatedUserAction
      if (post.hasAudience(UserSession.getCurrentUserName(request.session))) {
        Ok(views.html.posts.postDetails(post, commentForm, routes.PostController.addComment(postID)))
      } else {
        Forbidden("You're currently not allowed to view this Post!")
      }
    }.getOrElse(NotFound)
  }

  /* CREATE & EDIT POST */

  /**
   * Form for creating or editing a post.
   *
   * postID, posterName and filePath are set during form validation and submission handling
   * so they get default values or are ignored until then
   */
  private val postForm: Form[Post] =
    Form(mapping(
      "postID" -> default(longNumber, Post.UnstoredPostID),
      "posterName" -> default(nonEmptyText, User.UnstoredUserName),
      "filePath" -> ignored(Post.NoImage),
      "description" -> nonEmptyText,
      "postDate" -> date,
      "audienceOption" -> mapping(
        "option" -> nonEmptyText
          .verifying("Selected audience option is invalid.",  input => validateAudienceOptionOption(input)),
        "audience" -> list(text))
      (AudienceOption.apply)(AudienceOption.unapply),
      "likes" -> ignored(List[String]()), // initially always empty
      "comments" -> ignored(List[Comment]())
    )(Post.apply)(Post.unapply))

  /**
   *   Show form for creating a new post
   */
  def newPost() = authenticatedUserAction { implicit request =>
    val form = if (request.flash.get("ERROR").isDefined) {
      postForm.bind(request.flash.data)
    }
    else {
      postForm.fill(Post()) // prefill the form with default Post values
    }

    Ok(views.html.posts.editPost(form, routes.PostController.sharePost(), getAudienceOptions, getUserOptions))
  }

  /**
   *   Process post creation (i.e. share the newly created post)
   */
  def sharePost() = authenticatedUserAction(parse.multipartFormData) { implicit request =>
    // no need to validate audience if audience-options doesn't match audience selection (e.g. private post with an audience list),
    // since we will always check audience-options before using the audience list and storing the audience list could be helpful
    // to later prefill forms when changing the audience-options settings
    val originalPostForm = postForm.bindFromRequest()

    // we will always have a user since this is an authenticated action
    val currentUserName = UserSession.getCurrentUserName(request.session)

    originalPostForm.fold(
      hasErrors = { formWithErrors: Form[Post] =>
        BadRequest(views.html.posts.editPost(formWithErrors, routes.PostController.sharePost(), getAudienceOptions, getUserOptions))
      },
      success = { createdPost =>
        // handle the file upload (returns a form with possible errors and the final filename)
        val (postFilePath, newPostForm) =
          uploadImage(s"public/${FileStorage.UserPicturesDir}/$currentUserName", originalPostForm)

        if (newPostForm.hasErrors) {
          // delete the uploaded file again (if it exists), since we were unable to create the actual post
          if (postFilePath != Post.NoImage) FileStorage.deleteUserFile(currentUserName, postFilePath)

          BadRequest(views.html.posts.editPost(newPostForm, routes.PostController.sharePost(), getAudienceOptions, getUserOptions))
        }
        else {
          // set the post's poster name to the current user and the picture file path to the uploaded file's path
          val newPost = createdPost.copy(posterName = currentUserName, pictureFileName = postFilePath)
          val addedPost: Post = postDao.addPost(newPost) // store the Post
          if (postDao.isStoredPost(addedPost)) {
            // the post has been successfully created
            val message = "Successfully shared your post!"
            Redirect(routes.PostController.showPost(addedPost.postID)).
              flashing("SUCCESS" -> message)
          }
          else {
            InternalServerError("Something went wrong while storing the post. Please try again!")
          }
        }
      })
  }

  /**
   * Show page for editing an existing post
   *
   * @param postID the ID of the Post to edit
   */
  def editPost(postID : Long) = authenticatedUserAction { implicit request =>
    postDao.findByID(postID).map { post =>
      // prefill the form with the previously edited values or the current post values
      val form = if (request.flash.get("ERROR").isDefined) {
        postForm.bind(request.flash.data)
      }
      else {
        postForm.fill(post)
      }

      // we always have a username since this is an authenticated user action
      val currentUserName = UserSession.getCurrentUserName(request.session)

      // only allow editing your own posts
      if (post.posterName == currentUserName) {
        Ok(views.html.posts.editPost(form, routes.PostController.processEditPost(), getAudienceOptions,
          getUserOptions, addDeleteBtn = true, s"${FileStorage.UserPicturesDir}/$currentUserName/${post.pictureFileName}"))
      }
      else {
        Forbidden("You can only edit your own posts!")
      }
    }.getOrElse(NotFound)
  }

  /**
   * Process the submission of an edit post form
   */
  def processEditPost() = authenticatedUserAction(parse.multipartFormData) { implicit request =>
    val originalPostForm = postForm.bindFromRequest()

    originalPostForm.fold(
      hasErrors = { formWithErrors: Form[Post] =>
        // invalid data has been supplied
        BadRequest(views.html.posts.editPost(formWithErrors, routes.PostController.processEditPost(), getAudienceOptions, getUserOptions))
      },
      success = { createdPost =>
        postDao.findByID(createdPost.postID).map { oldPost =>
          // authentication check already occurred at this point (since it is an authenticatedUserAction)
          val currentUserName = UserSession.getCurrentUserName(request.session)
          val pictureChanged = request.body.file("picture").nonEmpty // has a new picture been selected or do we keep the old one
          val oldFileName = oldPost.pictureFileName
          var postFilePath = oldFileName // if the picture is not overwritten the old filePath remains
          var newPostForm = originalPostForm // newPostForm might have some new errors added to it later

          if (oldPost.posterName == currentUserName) {
            if (pictureChanged) {
              // handle the file upload (perform as late as possible)
              val (filePath, form) = uploadImage(s"public/${FileStorage.UserPicturesDir}/$currentUserName", originalPostForm)
              postFilePath = filePath
              newPostForm = form
            }

            if (newPostForm.hasErrors) {
              // we can only get here if the file failed to upload,
              // so no need to delete the newly uploaded file (since there is none)
              BadRequest(views.html.posts.editPost(newPostForm, routes.PostController.sharePost(), getAudienceOptions, getUserOptions))
            }
            else {
              // set the final file path of the new picture
              val editedPost = createdPost.copy(pictureFileName = postFilePath, comments = oldPost.comments, likes = oldPost.likes)

              if (postDao.updatePost(editedPost)) {
                // remove the previous picture if a new picture was successfully added
                if (pictureChanged) FileStorage.deleteUserFile(currentUserName, oldFileName)

                // show the newly edited post
                val message = "Your post has been modified. "
                Redirect(routes.PostController.showPost(editedPost.postID)).
                  flashing("SUCCESS" -> message)
              } else {
                InternalServerError("Something went wrong while updated the post! Please try again.")
              }
            }
          }
          else {
            Forbidden("You can only edit your own posts!")
          }
        }.getOrElse(NotFound)
      })
  }

  /**
   * Helper that tries to upload an image from a Post form.
   * It performs some additional form validation before uploading the file present in Multipart Form Data.
   *
   * @return A tuple with the final filePath of the image and a form that might contain additional errors is returned.
   *         In case file upload failed, the final filePath will be {@link Post.NoImage}.
   */
  def uploadImage(storeInDir: String, postForm : Form[Post])(implicit request: Request[MultipartFormData[play.api.libs.Files.TemporaryFile]]): (String, Form[Post]) = {
    // handle the file upload
    var newPostForm = postForm
    var postFilePath = Post.NoImage
    request.body
      .file("picture")
      .map { picture =>
        // only use the last part of the filename for storage
        // (to avoid writing to unauthorised directories through relative paths)
        val filename = Paths.get(picture.filename).getFileName
        val contentType = picture.contentType

        // validate the content type of the file
        val supportedContentTypes = Post.getSupportedFileTypes()
        if (contentType.isEmpty || !supportedContentTypes.contains(contentType.get)) {
          newPostForm = newPostForm.withError(FormError("picture", s"Only the following file types are supported: ${supportedContentTypes.mkString(", ")}"))
        }
        else {
          var pathToPicture = Paths.get(s"$storeInDir/$filename")

          val supportedFileExtensions = Post.getSupportedFileExtensions()
          val foundExtensions = supportedFileExtensions.filter(ext => pathToPicture.getFileName.toString.endsWith(ext))
          if (foundExtensions.isEmpty) {
            newPostForm = newPostForm.withError(FormError("picture", s"Only the following file extensions are supported: ${supportedFileExtensions.mkString(", ")}"))
          }
          else {
            // validate the file extension
            if (Files.exists(pathToPicture)) {
              // add an incrementing index if the file is already present (to avoid overwriting older pictures with the same name)
              pathToPicture =
                FileStorage.findAvailableFilePath(pathToPicture.getParent.toString,
                  pathToPicture.getFileName.toString.stripSuffix(foundExtensions.head), foundExtensions.head)
            }

            // ensure the path to the "permanent" picture directory exists and copy the temporary file to it
            Files.createDirectories(pathToPicture.getParent)
            picture.ref.copyTo(pathToPicture, replace = true)

            // save the filepath that will be stored in the post
            postFilePath = pathToPicture.getFileName.toString
          }
        }
      }
      .getOrElse {
        newPostForm = newPostForm.withError(FormError("picture", "A valid file is required."))
      }

    (postFilePath,newPostForm)
  }

  /**
   * Helper to verify whether the given option is a valid AudienceOption option
   */
  def validateAudienceOptionOption(option: String): Boolean = {
    option == AudienceOption.Public || option == AudienceOption.Private || option == AudienceOption.SelectiveAudience
  }

  /* DELETE POST */

  /**
   * Delete a post.
   *
   * Expected to be an AJAX call (DELETE).
   *
   * @param postID the ID of the post to delete
   */
  def deletePost(postID: Long) = authenticatedUserAction { implicit request =>
    postDao.findByID(postID).map { post => {
      // we always have a logged in user since this is an authenticatedUserAction
      val currentUserName = UserSession.getCurrentUserName(request.session)
      if (post.posterName == currentUserName) {
        // keep track of the picture file name in order to delete the actual file later on
        val postPicturePath = post.pictureFileName
        // delete the post
        if (postDao.deletePost(post)) {
          // delete the post's file
          FileStorage.deleteUserFile(currentUserName, postPicturePath)

          Ok("Your post has been deleted.")
        }
        else {
          InternalServerError("Something went wrong while deleting the post! Please try again.")
        }
      }
      else {
        Forbidden("You can only delete your own posts!")
      }
    }
    }.getOrElse(NotFound)
  }

  /* LIKE & UNLIKE */

  /**
   * Like or unlike a post (based on whether you currently liked it or not).
   * It is expected to be an AJAX call and will therefore return HTML for the updated like button.
   *
   * @param postID the ID of the Post to like/unlike
   */
  def toggleLike(postID: Long) = authenticatedUserAction { implicit request =>
    // authenticated user action so we should always have a user
    val currentUserName = UserSession.getCurrentUserName(request.session)

    // let the current user (un)like the post (if the given postID is valid)
    postDao.findByID(postID).map { post =>
      // toggle the like
      if (post.likedBy(currentUserName)) {
        post.removeLike(currentUserName)
      }
      else {
        post.addLike(currentUserName)
      }

      // store changes in the database
      if (postDao.updatePost(post)) {
        Ok(views.html.posts.like(post))
      }
      else {
        InternalServerError("The post could not be liked. Please try again!")
      }
    }.getOrElse(NotFound)
  }

  /* ADD COMMENT TO POST */

  // Comment Form
  // the commenterName will be filled in during processing so it can be ignored in the form
  private val commentForm: Form[Comment] =
    Form(mapping(
      "commenterName" -> ignored(User.UnstoredUserName),
      "contents" -> nonEmptyText,
    )(Comment.apply)(Comment.unapply))

  /**
   * Add a Comment to the specified post.
   * It is expected to be an AJAX call, so a view containing the new list of Comments is returned on success.
   *
   * @param postID, the ID of the post to add a Comment to
   */
  def addComment(postID : Long) = authenticatedUserAction { implicit request =>
    postDao.findByID(postID).map { post =>
      val newCommentForm = commentForm.bindFromRequest()

      // authenticated user action so we always have a user
      val currentUserName = UserSession.getCurrentUserName(request.session)

      newCommentForm.fold(
        hasErrors = { _ : Form[Comment] =>
          BadRequest("Invalid fields! Please try again.")
        },
        success = { createdComment =>
          // make the current user the commenter
          val newComment = createdComment.copy(commenterName = currentUserName)
          if (!postDao.addComment(post, newComment)) { // update the Post in the DB
            // something went wrong while storing the changes
            InternalServerError("Something went wrong while storing the comment! Please try again.")
          }
          else {
            // the comment was successfully added
            Ok(views.html.posts.commentsList(postDao.getComments(postID)))
          }
        }
      )
    }.getOrElse(NotFound)
  }

  /* POSTS OVERVIEW (FILTERED & SORTED) */

  // The default number of comments to show on a post overview page
  def overviewMaxComments = 2

  /**
   * Show a page with an overview of all visible posts sorted on Date
   */
  def showPostsOverview = getSortedPosts(PostDao.SortingField_Date, postsOnly = false)

  /**
   * Get a view with all Posts visible for the current user (sorted).
   *
   * @param sortOn Post field to sort on. Should be one of {@link PostDao.SortingField_Date} or {@link PostDao.SortingField_Date}
   * @param postsOnly Boolean specifying whether a list of HTML formatted posts should be returned (if true)
   * or a full HTML page (if false). Calls with postsOnly = true are intended to be used for AJAX calls.
   */
  def getSortedPosts(sortOn: String, postsOnly: Boolean) = Action { implicit request =>
    val currentUserName = UserSession.getCurrentUserName(request.session)

    // if the user is not logged in, don't show any posts
    val posts = if (currentUserName == User.UnstoredUserName) {
      List()
    }
    else {
      postDao.getVisiblePosts(currentUserName, sortOn)
    }

    if (postsOnly)
      Ok(views.html.posts.postsList(posts, overviewMaxComments)) // AJAX update
    else
      Ok(views.html.posts.postsOverview(posts, overviewMaxComments)) // Full HTML page
  }

  /* HELPERS */

  /**
   * Helper to get the userOptions for a select list (i.e. all users except the current one)
   */
  def getUserOptions(implicit request: Request[Any]): List[(String, String)] = {
    val currentUserName = UserSession.getCurrentUserName(request.session)
    userDao.filterUsers(user => user.username != currentUserName).map {
      user => (user.username, user.username)
    }
  }

  /**
   * Helper to get the audioOptions for a select list (i.e. ALL, NONE or SELECTED AUDIENCE)
   */
  def getAudienceOptions(implicit request: Request[Any]): List[(String, String)] = {
    List((AudienceOption.Public, "All users"), (AudienceOption.Private, "Only me"),
      (AudienceOption.SelectiveAudience, "The selected users"))
  }
}
