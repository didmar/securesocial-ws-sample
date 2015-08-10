package service

import org.joda.time.DateTime
import play.api.Play
import play.api.mvc.Results.Ok
import play.api.mvc.{ Result, _ }
import securesocial.core.authenticator._
import play.api.Logger

import scala.concurrent.Future

/**
 * A http header based authenticator. This authenticator works using the X-Auth-Token header in the http request
 * to track authenticated users. Since the token is only an id the rest of the user data is stored using an
 * instance of the AuthenticatorStore.
 *
 * @param id the authenticator id
 * @param user the user this authenticator is associated with
 * @param expirationDate the expiration date
 * @param lastUsed the last time the authenticator was used
 * @param creationDate the authenticator creation time
 * @param store the authenticator store where instances of this authenticator are persisted
 * @tparam U the user type (defined by the application using the module)
 *
 * @see AuthenticatorStore
 * @see RuntimeEnvironment
 */
case class WSAuthenticator[U](id: String, user: U, expirationDate: DateTime,
  lastUsed: DateTime,
  creationDate: DateTime,
  @transient store: AuthenticatorStore[WSAuthenticator[U]])
    extends StoreBackedAuthenticator[U, WSAuthenticator[U]] {

  override val idleTimeoutInMinutes = WSAuthenticator.idleTimeout
  override val absoluteTimeoutInSeconds = WSAuthenticator.absoluteTimeoutInSeconds
  /**
   * Returns a copy of this authenticator with the given last used time
   *
   * @param time the new time
   * @return the modified authenticator
   */
  def withLastUsedTime(time: DateTime): WSAuthenticator[U] = this.copy[U](lastUsed = time)

  /**
   * Returns a copy of this Authenticator with the given user
   *
   * @param user the new user
   * @return the modified authenticator
   */
  def withUser(user: U): WSAuthenticator[U] = this.copy[U](user = user)

  /**
   * Starts an authenticated session by returning the authenticator id
   *
   * @param result the result that is about to be sent to the client
   * @return the result with the authenticator header set
   */
  override def starting(result: Result): Future[Result] = {
    Future.successful { Ok(id) }
  }
}

/**
 * An authenticator builder. It can create an Authenticator instance from an http request or from a user object
 *
 * @param store the store where instances of the WSAuthenticator class are persisted.
 * @param generator a session id generator
 * @tparam U the user object type
 */
class WSAuthenticatorBuilder[U](store: AuthenticatorStore[WSAuthenticator[U]], generator: IdGenerator)
    extends AuthenticatorBuilder[U] {

  import store.executionContext

  val id = WSAuthenticator.Id

  /**
   * Creates an instance of a WSAuthenticator from the http request
   *
   * @param request the incoming request
   * @return an optional WSAuthenticator instance.
   */
  override def fromRequest(request: RequestHeader): Future[Option[WSAuthenticator[U]]] = {
    Logger.debug(s"fromRequest ${request}")
    request.headers.get(WSAuthenticator.headerName) match {
      case Some(value) => {
        Logger.debug(s"token = $value")
        store.find(value).map { retrieved =>
          retrieved.map { _.copy(store = store) }
        }
      }
      case None => Future.successful(None)
    }
  }

  /**
   * Creates an instance of a WSAuthenticator from a user object.
   *
   * @param user the user
   * @return a WSAuthenticator instance.
   */
  override def fromUser(user: U): Future[WSAuthenticator[U]] = {
    Logger.debug(s"fromUser ${user}")
    generator.generate.flatMap {
      id =>
        val now = DateTime.now()
        val expirationDate = now.plusMinutes(WSAuthenticator.absoluteTimeout)
        val authenticator = WSAuthenticator(id, user, expirationDate, now, now, store)
        Logger.debug(s"authenticator=$authenticator")
        store.save(authenticator, WSAuthenticator.absoluteTimeoutInSeconds)
    }
  }
}

object WSAuthenticator {
  import play.api.Play.current

  val Id = "wstoken"
  val HeaderNameKey = "securesocial.auth-header.name"

  // default values
  val DefaultHeaderName = "X-Auth-Token"

  lazy val headerName = Play.application.configuration.getString(HeaderNameKey).getOrElse(DefaultHeaderName)
  // using the same properties than the CookieBased authenticator for now.
  lazy val idleTimeout = CookieAuthenticator.idleTimeout
  lazy val absoluteTimeout = CookieAuthenticator.absoluteTimeout
  lazy val absoluteTimeoutInSeconds = CookieAuthenticator.absoluteTimeoutInSeconds
}
