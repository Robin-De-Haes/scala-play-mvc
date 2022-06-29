package models

@javax.inject.Singleton
object Global {
    // Key for storing the currently logged in user
    val SESSION_USERNAME_KEY = "username"
    // Key for showing a message through a URL query
    val QUERY_MESSAGE_KEY = "message"
}

