/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.{Action, Result, RequestHeader}
import play.filters.csrf._
import scala.concurrent.{Await, Future}
import securesocial.controllers.MailTokenBasedOperations
import securesocial.core._
import securesocial.core.providers.MailToken
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils._
import securesocial.core.services.SaveMode
import service.WSAuthenticator
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * A default WSRegistration controller that uses the BasicProfile as the user type
 *
 * @param env the environment
 */
class WSRegistration(override implicit val env: RuntimeEnvironment[BasicProfile]) extends WSRegistrationTrait[BasicProfile]

/**
 * A trait that provides the means to handle user registration
 *
 * @tparam U the user type
 */
trait WSRegistrationTrait[U] extends MailTokenBasedOperations[U] {

  import controllers.WSRegistration._
  implicit val wsRegistrationInfoFormats = Json.format[WSRegistrationInfo]

  private val logger = play.api.Logger("controllers.WSRegistration")

  val providerId = UsernamePasswordProvider.UsernamePassword

  val UserName = "userName"
  val FirstName = "firstName"
  val LastName = "lastName"

  def handleStartSignUp = Action.async { implicit request =>
    request.body.asText.map( rawEmail => {
      val email = rawEmail.toLowerCase
      // check if there is already an account for this email address
      env.userService.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword).flatMap {
        _ match {
          case Some(user) => {
            // user signed up already, send an email offering to login/recover password
            env.mailer.sendAlreadyRegisteredEmail(user)
            Future.successful(Ok("Email sent"))
          }
          case None => {
            createToken(email, isSignUp = true).map { token =>
              env.mailer.sendSignUpEmail(email, token.uuid)
              env.userService.saveToken(token)
              Ok("Email sent")
            }
          }
        }
      }
    }).getOrElse(Future.successful(BadRequest("Email must be provided in the body")))
  }

  def createNewUser(info: WSRegistrationInfo, email: String): BasicProfile = {
    val id = if (UsernamePasswordProvider.withUserNameSupport) {
      info.userName.getOrElse(email)
    } else {
      email
    }
    BasicProfile (
      providerId,
      id,
      Some(info.firstName),
      Some(info.lastName),
      Some("%s %s".format(info.firstName, info.lastName)),
      Some(email),
      None,
      AuthenticationMethod.UserPassword,
      passwordInfo = Some(env.currentHasher.hash(info.password))
    )
  }

  private def getAvatar(newUser: BasicProfile, email: String): Future[BasicProfile] = {
    env.avatarService.map {
      _.urlFor(email).map { url =>
        if (url != newUser.avatarUrl) newUser.copy(avatarUrl = url) else newUser
      }
    }.getOrElse(Future.successful(newUser))
  }

  /**
   * Handles sign up with a token and a JSON object
   */
  def handleSignUp(token: String) = Action.async(parse.json) { implicit request =>
    val jsValue: JsValue = request.body.asInstanceOf[JsValue]
    jsValue.validate[WSRegistrationInfo] match {
      case JsError(errors) => Future.successful(BadRequest("Invalid registration info"))
      case JsSuccess(info: WSRegistrationInfo, path: JsPath) => withToken(token, true, { t =>
        val newUser = createNewUser(info, t.email)
        val withAvatar = getAvatar(newUser, t.email)
        import securesocial.core.utils._
        val result: Future[Future[Result]] = for (
          toSave <- withAvatar;
          saved <- env.userService.save(toSave, SaveMode.SignUp);
          deleted <- env.userService.deleteToken(t.uuid)
        ) yield {
          if (UsernamePasswordProvider.sendWelcomeEmail) {
            env.mailer.sendWelcomeEmail(newUser)
          }
          val eventSession = Events.fire(new SignUpEvent(saved)).getOrElse(request.session)
          if (UsernamePasswordProvider.signupSkipLogin) {
            val res: Future[Result] = env.authenticatorService.find(WSAuthenticator.Id) match {
              case Some(authenticatorBuilder) => {
                authenticatorBuilder.fromUser(saved).flatMap { authenticator =>
                  authenticator.starting(Ok("dummy"))
                }
              }
              case None => {
                logger.error("[securesocial] There isn't WSAuthenticator registered in the RuntimeEnvironment")
                Future.successful(InternalServerError)
              }
            }
            res
          } else {
            Future.successful(Ok("Signed up sucessfuly")) //confirmationResult().flashing(Success -> Messages(SignUpDone)).withSession(eventSession))
          }
        }
        result.flatMap(f => f)
      })
    }
  }

  /**
   * Helper method to execute actions where a token needs to be retrieved from
   * the backing store
   *
   * @param token the token id
   * @param isSignUp a boolean indicating if the token is used for a signup or password reset operation
   * @param f the function that gets invoked if the token exists
   * @param request the current request
   * @return the action result
   */
  private def withToken(token: String, isSignUp: Boolean,
      f: MailToken => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    env.userService.findToken(token).flatMap {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => f(t)
      case _ =>
        Future.successful(Ok("Invalid link !"))
    }
  }
}

object WSRegistration {
  val UserNameAlreadyTaken = "securesocial.signup.userNameAlreadyTaken"
  val ThankYouCheckEmail = "securesocial.signup.thankYouCheckEmail"
  val InvalidLink = "securesocial.signup.invalidLink"
  val SignUpDone = "securesocial.signup.signUpDone"
  val Password = "password"
  val Password1 = "password1"
  val Password2 = "password2"

  val PasswordsDoNotMatch = "securesocial.signup.passwordsDoNotMatch"
}

/**
 * The data collected during the registration process
 *
 * @param userName the username
 * @param firstName the first name
 * @param lastName the last name
 * @param password the password
 */
case class WSRegistrationInfo(userName: Option[String], firstName: String, lastName: String, password: String)
