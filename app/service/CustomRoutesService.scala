/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package service

import com.typesafe.config.ConfigFactory
import play.api.mvc.{ Call, RequestHeader }
import securesocial.core.IdentityProvider
import securesocial.core.services.RoutesService

/**
 * A RoutesService implementation which customizes the redirect URL
 */
class CustomRoutesService extends RoutesService {
  lazy val conf = play.api.Play.current.configuration

  val FaviconKey = "securesocial.faviconPath"
  val JQueryKey = "securesocial.jqueryPath"
  val CustomCssKey = "securesocial.customCssPath"
  val DefaultFaviconPath = "images/favicon.png"
  val DefaultJqueryPath = "javascripts/jquery-1.7.1.min.js"

  val removeTrailingSlash: String => String = s =>
    if(s.endsWith("/")) s.substring(0, s.length() - 1) else s

  val config = ConfigFactory.load()
  val baseURL = removeTrailingSlash(config.getString("application.mailer.baseURL"))

  override def loginPageUrl(implicit req: RequestHeader): String = {
    baseURL + "/login"
  }

  override def startSignUpUrl(implicit req: RequestHeader): String = {
    baseURL + "/signup"
  }

  override def handleStartSignUpUrl(implicit req: RequestHeader): String = {
    baseURL + "/signup"
  }

  override def signUpUrl(mailToken: String)(implicit req: RequestHeader): String = {
    baseURL + "/signup/" + mailToken
  }

  override def handleSignUpUrl(mailToken: String)(implicit req: RequestHeader): String = {
    baseURL + "/signup/" + mailToken
  }

  override def startResetPasswordUrl(implicit request: RequestHeader): String = {
    baseURL + "/reset"
  }

  override def handleStartResetPasswordUrl(implicit req: RequestHeader): String = {
    baseURL + "/reset"
  }

  override def resetPasswordUrl(mailToken: String)(implicit req: RequestHeader): String = {
    baseURL + "/reset"
  }

  override def handleResetPasswordUrl(mailToken: String)(implicit req: RequestHeader): String = {
    baseURL + "/reset/" + mailToken
  }

  override def passwordChangeUrl(implicit req: RequestHeader): String = {
    baseURL + "/password-change"
  }

  override def handlePasswordChangeUrl(implicit req: RequestHeader): String = {
    baseURL +"/password-change"
  }

  override def authenticationUrl(provider: String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String = {
    baseURL + "/login/" + provider + redirectTo.map(u => "?redirect-to=" + u).getOrElse("")
  }

  protected def valueFor(key: String, default: String) = {
    val value = conf.getString(key).getOrElse(default)
    securesocial.controllers.routes.Assets.at(value)
  }

  /**
   * Loads the Favicon to use from configuration, using a default one if not provided
   * @return the path to Favicon file to use
   */
  override val faviconPath = valueFor(FaviconKey, DefaultFaviconPath)

  /**
   * Loads the Jquery file to use from configuration, using a default one if not provided
   * @return the path to Jquery file to use
   */
  override val jqueryPath = valueFor(JQueryKey, DefaultJqueryPath)

  /**
   * Loads the Custom Css file to use from configuration. If there is none define, none will be used
   * @return Option containing a custom css file or None
   */
  override val customCssPath: Option[Call] = {
    val path = conf.getString(CustomCssKey).map(securesocial.controllers.routes.Assets.at)
    path
  }
}

