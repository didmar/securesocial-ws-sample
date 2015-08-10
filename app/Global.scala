import java.lang.reflect.Constructor
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator._
import securesocial.core.services._
import service.{MyEventListener, InMemoryUserService, WSAuthenticatorBuilder}
import models.User
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {

  /**
   * The runtime environment for this sample app.
   */
  object MyRuntimeEnvironment extends RuntimeEnvironment.Default[User.User] {
    override implicit val executionContext = play.api.libs.concurrent.Execution.defaultContext
    override lazy val userService: InMemoryUserService = new InMemoryUserService()
    override lazy val eventListeners = List(new MyEventListener())
    override lazy val authenticatorService = new AuthenticatorService(
      new CookieAuthenticatorBuilder[User.User](new AuthenticatorStore.Default(cacheService), idGenerator),
      new WSAuthenticatorBuilder[User.User](new AuthenticatorStore.Default(cacheService), idGenerator)
    )
  }

  /**
   * An implementation that checks if the controller expects a RuntimeEnvironment and
   * passes the instance to it if required.
   *
   * This can be replaced by any DI framework to inject it differently.
   *
   * @param controllerClass
   * @tparam A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[User.User]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(MyRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }
}
