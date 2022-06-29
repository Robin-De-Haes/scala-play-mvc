package models

import javax.inject.Inject

@javax.inject.Singleton
class UserDao @Inject()() {
    // dummy in-memory representation of DB users
    // passwords should be encrypted in an actual DB implementation
    var users = Seq(
        User("Robin", "Robin1"),
        User("Coen", "Coen1"),
        User("Camilo", "Camilo1"),
        User("Ahmed", "Ahmed1")
    )

    // dummy implementation for querying DB for existing users satisfying the specified filter
    def filterUsers(userFilter : User => Boolean) : List[User] = {
        users.filter(userFilter).toList
    }

    // dummy implementation for querying DB for an existing user
    def findByUserName(userName: String): Option[User] = {
        users.find(_.username.equalsIgnoreCase(userName))
    }

    // dummy implementation to add a user to DB
    def addUser(user: User): Boolean = {
        users = users :+ user

        // dummy implementation of DB storage being successful
        users.contains(user)
    }

    // dummy implementation to check if a user with the specified username exists in the DB (for username uniqueness)
    def isExistingUser(userName: String) : Boolean = {
        users.exists(_.username.equalsIgnoreCase(userName))
    }
}


