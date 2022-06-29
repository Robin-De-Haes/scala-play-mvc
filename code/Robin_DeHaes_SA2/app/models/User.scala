package models

object User {
  // default name for Users not stored in the database,
  // mostly used to indicate the current User is unauthenticated
  val UnstoredUserName : String = "UNSTORED_USER"
}

// Class representing a social network User
case class User(username: String = User.UnstoredUserName, password: String = "")
