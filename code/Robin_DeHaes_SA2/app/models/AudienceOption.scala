package models

object AudienceOption {
  // We used String identifiers for easy use in scala.html forms
  val Public = "AUDIENCE_ALL"
  val Private = "AUDIENCE_NONE"
  val SelectiveAudience = "AUDIENCE_SELECTION"
}

// Helper class used in Post to specify the audience for which a Post is visible
case class AudienceOption(var option : String = AudienceOption.Public, var audience : List[String] = List())