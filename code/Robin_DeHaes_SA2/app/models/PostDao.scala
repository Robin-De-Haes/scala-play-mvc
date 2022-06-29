package models

import javax.inject.Inject

object PostDao {
  // Fields that are available for sorting on
  val SortingField_Date = "date"
  val SortingField_LikeCount = "likes"
}

@javax.inject.Singleton
class PostDao @Inject()(){
  // dummy implementation of a DB counter for an auto-incrementing Post ID
  var currentPostID = 6

  // dummy in-memory representation of DB posts
  var posts: Seq[Post] = Seq(
    Post(1, "Robin", "spartacus1.png", "Spartacus Run Logo", Post.DefaultDateFormat.parse("17/03/2020"),
      AudienceOption(AudienceOption.Public, List()), List(), List()),
    Post(2, "Robin", "spartacus2.jpg", "We are Spartacus Run finishers!", Post.DefaultDateFormat.parse("17/03/2021"),
      AudienceOption(AudienceOption.Public, List("Camilo", "Ahmed")), List("Camilo","Ahmed","Coen"),
      List(Comment("Ahmed", "Great job!"))),
    Post(3, "Robin", "robin1.jpg", "Lost in deep thoughts...", Post.DefaultDateFormat.parse("15/09/2020"),
      AudienceOption(AudienceOption.Private, List()), List("Camilo"),
      List(Comment("Camilo", "Are you thinking about Software Architectures?"))),
    Post(4, "Ahmed", "first-post.jpg", "This is my first post!", Post.DefaultDateFormat.parse("01/01/2021"),
      AudienceOption(AudienceOption.SelectiveAudience, List("Camilo","Coen")), List(),
      List(Comment("Robin", "Welcome Ahmed!"), Comment("Camilo", "You're a very welcome addition."), Comment("Coen", "Happy to have you here!"))),
    Post(5, "Camilo", "vub-campus.jpg", "Free University of Brussels", Post.DefaultDateFormat.parse("02/06/2021"),
      AudienceOption(AudienceOption.Public, List()), List("Ahmed", "Robin"),
      List(Comment("Robin", "I've learned a lot here"), Comment("Coen", "Yeah, the assistants there are pretty great!"),
        Comment("Camilo", "We sure are!"), Comment("Ahmed", "I agree!"))),
    Post(6, "Camilo", "coen1.jpg", "I am assisting this man for the course of Software Architectures this year.", Post.DefaultDateFormat.parse("01/02/2021"),
      AudienceOption(AudienceOption.Public, List()), List("Coen","Robin"),
      List(Comment("Ahmed", "Nice, I am assisting him as well!"), Comment("Coen", "And you're both doing a good job!")))
  )

  // Helper to get a sorted list of posts that are visible for the given user,
  // possibly additionally filtered with additionalFilter and sorted according to the given sortOn option
  def getVisiblePosts(userName: String, sortOn : String, additionalFilter : Post => Boolean = (_ => true)) : List[Post] = {
    // filter the posts on user visibility (and the additional filter)
    val filteredPosts = posts.withFilter(getAudienceOptionFilter(userName)).withFilter(additionalFilter).map(identity).toList

    // reverse sorting (more likes or more recent get showed first)
    val sortedPosts = sortOn match {
      case PostDao.SortingField_LikeCount =>
        filteredPosts.sortBy(currentPost => (currentPost.getLikeCount, currentPost.postDate)).reverse
      case PostDao.SortingField_Date =>
        filteredPosts.sortBy(currentPost => (currentPost.postDate, currentPost.getLikeCount)).reverse
    }

    sortedPosts
  }

  // dummy implementation of looking up a Post in the DB
  def findByID(postID: Long): Option[Post] = {
    posts.find(_.postID == postID)
  }

  // dummy implementation of adding a Post to the DB
  def addPost(post: Post) : Post = {
    // add the post to the DB (but with a valid auto-incremented post ID)
    val storedPost = post.copy(postID = generatePostID())
    posts = posts :+ storedPost

    // dummy implementation of DB storage being successful,
    // the stored Post is returned in case the application needs the stored Post with its valid postID later on
    if (posts.contains(storedPost)) storedPost else post
  }

  // dummy implementation of updating a Post in DB storage
  def updatePost(post: Post) : Boolean = {
    val postIndex : Int = posts.indexWhere(currentPost => currentPost.postID == post.postID)
    if (postIndex < 0) return false

    posts = posts.updated(postIndex, post)
    posts.contains(post) // DB operation is successful
  }

  // dummy implementation of deleting a Post in DB storage
  def deletePost(post: Post) : Boolean = {
    val originalSize = posts.size
    posts = posts.filter(currentPost => currentPost.postID != post.postID)

    posts.size < originalSize // DB operation is successful
  }

  // dummy implementation of an auto-incrementing userID
  def generatePostID() : Long = {
    currentPostID = currentPostID + 1
    currentPostID
  }

  // dummy DB implementation to add a comment to the post
  def addComment(post: Post, comment: Comment): Boolean = {
    post.comments = post.comments :+ comment
    updatePost(post)
  }

  // dummy DB implementation to get all comments for a post
  def getComments(postID : Long) : List[Comment] = {
    findByID(postID).map(_.comments).getOrElse(List())
  }

  // helper method to check if a post represents an already stored post (with a valid DB ID)
  def isStoredPost(post: Post): Boolean = {
    post.postID != Post.UnstoredPostID
  }

  // helper method to retrieve a filter based on a Post's audience option
  def getAudienceOptionFilter(currentUserName: String): Post => Boolean = _.hasAudience(currentUserName)
}
