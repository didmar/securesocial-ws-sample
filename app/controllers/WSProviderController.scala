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

import play.api.i18n.Messages
import play.api.Logger
import play.api.mvc._
import play.api.Play
import play.api.Play.current
import scala.concurrent.Future
import securesocial.controllers.BaseProviderController
import securesocial.core._
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.services.SaveMode
import securesocial.core.utils._
import service.{WSAuthenticator, WSAuthenticatorBuilder}

class WSProviderController(override implicit val env: RuntimeEnvironment[BasicProfile])
  extends WSProviderControllerTrait[BasicProfile]

trait WSProviderControllerTrait[U] extends SecureSocial[U] {

  import securesocial.controllers.ProviderControllerHelper.{ logger, toUrl }

  /**
   * The authentication entry point for GET requests
   *
   * @param provider The id of the provider that needs to handle the call
   */
  def authenticate(provider: String) = handleAuth(provider)

  /**
   * The authentication entry point for POST requests
   *
   * @param provider The id of the provider that needs to handle the call
   */
  def authenticateByPost(provider: String) = handleAuth(provider)

  private def getWSAuthenticatorBuilder(): Option[WSAuthenticatorBuilder[U]] = {
    env.authenticatorService.find(WSAuthenticator.Id).map(_.asInstanceOf[WSAuthenticatorBuilder[U]])
  }

  private def getUsernamePasswordProvider(): Option[UsernamePasswordProvider[U]] = {
    env.providers.get(UsernamePasswordProvider.UsernamePassword).map(_.asInstanceOf[UsernamePasswordProvider[U]])
  }

  /**
   * Common method to handle GET and POST authentication requests
   *
   * @param provider the provider that needs to handle the flow
   */
  private def handleAuth(provider: String) = UserAwareAction.async { implicit request =>
    val authenticationFlow = request.user.isEmpty

    getUsernamePasswordProvider().map { userpassProvider =>
      userpassProvider.authenticateForApi(request).flatMap {
        case denied: AuthenticationResult.AccessDenied => {
          Future.successful(Forbidden)
        }
        case failed: AuthenticationResult.Failed =>
          // Invalid credentials
          logger.info(s"Authentication failed, reason: ${failed.error}")
          Future.successful(NotFound)
        case flow: AuthenticationResult.NavigationFlow => Future.successful {
          Logger.debug("case flow")
          flow.result
        }
        case authenticated: AuthenticationResult.Authenticated => {
          Logger.debug("handleAuth authenticated")
          if (authenticationFlow) {
            val profile = authenticated.profile
            env.userService.find(profile.providerId, profile.userId).flatMap { maybeExisting =>
              val mode = if (maybeExisting.isDefined) SaveMode.LoggedIn else SaveMode.SignUp
              env.userService.save(authenticated.profile, mode).flatMap { userForAction =>
                logger.debug(s"[securesocial] user completed authentication: provider = ${profile.providerId}, userId: ${profile.userId}, mode = $mode")
                val evt = if (mode == SaveMode.LoggedIn) new LoginEvent(userForAction) else new SignUpEvent(userForAction)
                val sessionAfterEvents = Events.fire(evt).getOrElse(request.session)
                Logger.debug("handleAuth builder().fromUser")
                getWSAuthenticatorBuilder().map {
                  _.fromUser(userForAction).flatMap { authenticator =>
                    // The WSAuthenticator will return a result with the token
                    authenticator.starting(Ok("dummy"))
                  }
                }.getOrElse {
                  logger.error(s"[securesocial] missing WSAuthenticatorBuilder")
                  Future.successful(InternalServerError)
                }
              }
            }
          } else {
            request.user match {
              case Some(currentUser) =>
                for (
                  linked <- env.userService.link(currentUser, authenticated.profile);
                  updatedAuthenticator <- request.authenticator.get.updateUser(linked);
                  result <- Redirect(toUrl(request.session)).withSession(request.session -
                    SecureSocial.OriginalUrlKey -
                    IdentityProvider.SessionId -
                    OAuth1Provider.CacheKey).touchingAuthenticator(updatedAuthenticator)
                ) yield {
                  logger.debug(s"[securesocial] linked $currentUser to: providerId = ${authenticated.profile.providerId}")
                  result
                }
              case _ =>
                Future.successful(Unauthorized)
            }
          }
        }
      } recover {
        case e => {
          logger.error("Unable to log user in. An exception was thrown", e)
          InternalServerError
        }
      }
    }.getOrElse {
      logger.error("[securesocial] missing UsernamePasswordProvider")
      Future.successful(InternalServerError)
    }
  }
}
