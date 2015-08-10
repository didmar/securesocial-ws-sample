package controllers

import models.User
import play.api._
import play.api.mvc.Action
import securesocial.core.{SecureSocial, RuntimeEnvironment}

class Application(override implicit val env: RuntimeEnvironment[User.User])
  extends securesocial.core.SecureSocial[User.User] {

  def index = Action { implicit request =>
    Ok("This does not require authentification")
  }

  def test = SecuredAction { implicit request =>
    val userId = request.user.main.userId
    Ok(s"Your id is $userId")
  }
}
