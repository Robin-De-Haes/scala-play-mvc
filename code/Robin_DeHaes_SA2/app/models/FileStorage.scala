package models

import java.nio.file.{Files, Path, Paths}

// Object containing useful general helpers for storing files
@javax.inject.Singleton
object FileStorage {
  // path to the folder with user pictures (pictures from user posts)
  val UserPicturesDir = "user-pictures"

  /**
   * Find a filepath that is currently not used (to prevent unwanted overwriting of files)
   *
   * @param parentPath the directory path where the file is located at
   * @param fileNameNoExtension the name of the file without its extension
   * @param extension the file extension
   */
  def findAvailableFilePath(parentPath: String, fileNameNoExtension: String, extension: String): Path = {
    var index = 0
    var currentPath = Paths.get(parentPath, fileNameNoExtension.concat(index.toString).concat(extension))
    while (Files.exists(currentPath)) {
      index = index + 1
      currentPath = Paths.get(parentPath, fileNameNoExtension.concat(index.toString).concat(extension))
    }
    currentPath
  }

  /**
   * Delete the file with name fileName uploaded by the User with userName,
   * i.e. delete the file from the user's personal folder.
   *
   * @param userName name of the User to delete the file for
   * @param fileName name of the File that should be deleted
   */
  def deleteUserFile(userName : String, fileName : String): Unit = {
    Files.delete(Paths.get(s"public/$UserPicturesDir/${userName}/$fileName"))
  }
}
