package models

import securesocial.core._
import play.api.libs.functional.syntax._
import play.api.libs.json._

// a simple User class that can have multiple identities
object User {
  case class User(main: BasicProfile, identities: List[BasicProfile])
  case class UserUID(providerId: String, userId: String)

  val uuid: User => UserUID = { u =>
    UserUID(u.main.providerId, u.main.userId)
  }

  val uuidToString: UserUID => String = { uuid =>
    uuid.providerId + "/" + uuid.userId
  }

  implicit val oAuth1InfoReads: Reads[OAuth1Info] = (
    (__ \ "token").read[String] and
    (__ \ "secret").read[String]
  )(OAuth1Info.apply _)

  implicit val oAuth1InfoWrites = new Writes[OAuth1Info] {
    def writes(o: OAuth1Info): JsValue = {
      Json.obj(
        "token" -> o.token,
        "secret" -> o.secret
      )
    }
  }

  implicit val oAuth2InfoReads: Reads[OAuth2Info] = (
    (__ \ "accessToken").read[String] and
    (__ \ "tokenType").read[Option[String]] and
    (__ \ "expiresIn").read[Option[Int]] and
    (__ \ "refreshToken").read[Option[String]]
  )(OAuth2Info.apply _)

  implicit val oAuth2InfoWrites = new Writes[OAuth2Info] {
    def writes(o: OAuth2Info): JsValue = {
      Json.obj(
        "accessToken" -> o.accessToken,
        "tokenType" -> o.tokenType,
        "expiresIn" -> o.expiresIn,
        "refreshToken" -> o.refreshToken
      )
    }
  }

  implicit val passwordReads: Reads[PasswordInfo] = (
    (__ \ "hasher").read[String] and
    (__ \ "password").read[String] and
    (__ \ "salt").read[Option[String]]
  )(PasswordInfo.apply _)

  implicit val passwordWrites = new Writes[PasswordInfo] {
    def writes(p: PasswordInfo): JsValue = {
      Json.obj(
        "hasher" -> p.hasher,
        "password" -> p.password,
        "salt" -> p.salt
      )
    }
  }

  implicit val basicProfileReads: Reads[BasicProfile] = (
    (__ \ "providerId").read[String] and
    (__ \ "userId").read[String] and
    (__ \ "firstName").read[Option[String]] and
    (__ \ "lastName").read[Option[String]] and
    (__ \ "fullName").read[Option[String]] and
    (__ \ "email").read[Option[String]] and
    (__ \ "avatarUrl").read[Option[String]] and
    (__ \ "authMethod").read[String].map(AuthenticationMethod.apply _) and
    (__ \ "oAuth1Info").read[Option[OAuth1Info]] and
    (__ \ "oAuth2Info").read[Option[OAuth2Info]] and
    (__ \ "passwordInfo").read[Option[PasswordInfo]]
  )(BasicProfile.apply _)

  implicit val basicProfileWrites = new Writes[BasicProfile] {
    def writes(b: BasicProfile): JsValue = {
      Json.obj(
        "providerId" -> b.providerId,
        "userId" -> b.userId,
        "firstName" -> b.firstName,
        "lastName" -> b.lastName,
        "fullName" -> b.fullName,
        "email" -> b.email,
        "avatarUrl" -> b.avatarUrl,
        "authMethod" -> b.authMethod.method,
        "oAuth1Info" -> b.oAuth1Info,
        "oAuth2Info" -> b.oAuth2Info,
        "passwordInfo" -> b.passwordInfo
      )
    }
  }

  implicit val userUIDReads: Reads[UserUID] = (
    (__ \ "providerId").read[String] and
    (__ \ "userId").read[String]
  )(UserUID.apply _)

  implicit val userUIDWrites = new Writes[UserUID] {
    def writes(uuid: UserUID): JsValue = {
      Json.obj(
        "providerId" -> uuid.providerId,
        "userId" -> uuid.userId
      )
    }
  }

  implicit val userReads: Reads[User] = (
    (__ \ "main").read[BasicProfile] and
    (__ \ "identities").read[List[BasicProfile]]
  )(User.apply _)

  implicit val userWrites = new Writes[User] {
    def writes(u: User): JsValue = {
      Json.obj(
        "_id" -> uuid(u),
        "main" -> u.main,
        "identities" -> u.identities
      )
    }
  }
}
