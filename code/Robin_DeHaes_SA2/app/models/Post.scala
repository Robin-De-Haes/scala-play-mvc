package models

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

object Post {
  val DefaultDateFormat = new SimpleDateFormat("dd/MM/y") // default formatter for postDate
  val NoImage = "images/no-image.jpg" // default image that could be shown if no image is available (currently unneeded)
  val UnstoredPostID: Long = -1 // default ID for newly created Posts (gets filled with database ID after storage)

  // helper methods to get the file formats currently accepted for creating a post
  def getSupportedFileTypes(): List[String] = List("image/png", "image/gif", "image/jpeg")
  def getSupportedFileExtensions(): List[String] = List(".jpg", ".jpeg", ".png", ".gif")
}

// Class representing social network Posts
// The Lists of likes and comments have been added to the constructor for easy creation of "database Posts", but in
// most regular use cases these fields will be empty at construction.
case class Post (postID: Long = Post.UnstoredPostID,
                 posterName: String = User.UnstoredUserName,
                 var pictureFileName : String = Post.NoImage,
                 var description: String = "", var postDate : Date = Calendar.getInstance().getTime,
                 var audienceOption: AudienceOption = AudienceOption(),
                 var likes: List[String] = List.empty,
                 var comments: List[Comment] = List.empty) {

  /**
   * Get the number of likes the Post has.
   */
  def getLikeCount: Int = {
    likes.size
  }

  /**
   * Check whether the User with the given userName liked this Post.
   *
   * @param userName userName of the User to check the likes for.
   */
  def likedBy(userName: String) : Boolean = {
    likes.contains(userName)
  }

  /**
   * Add a like from liker to the Post.
   *
   * @param liker the userName of a User that likes this Post
   */
  def addLike(liker : String): Unit = {
    likes = likes :+ liker
  }

  /**
   * Remove the like from liker from the Post.
   *
   * @param unliker the userName of a User that unlikes this Post
   */
  def removeLike(unliker : String) : Unit = {
    likes = likes.filter(_ != unliker)
  }

  /**
   * Checks if the User referenced by the specified userName belongs to the Post's audience.
   */
  def hasAudience(userName: String) : Boolean = {
    audienceOption.option match {
      case AudienceOption.Private => posterName == userName
      case AudienceOption.SelectiveAudience => (posterName == userName || audienceOption.audience.contains (userName) )
      case _ => true // AudienceOption.Public
    }
  }

  /**
   * Get the formatted postDate of the Post.
   * This is mainly used in the views.
   *
   * @param format the formatting to use on the postDate.
   */
  def getFormattedPostDate(format : SimpleDateFormat = Post.DefaultDateFormat): String =  {
    format.format(postDate)
  }
}
