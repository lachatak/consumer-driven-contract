package scala.org.kaloz.cdc.security.contracts

import org.apache.http.HttpStatus
import org.kaloz.cdc.security.handler.{AdvertApi, UsersApi}
import org.kaloz.cdc.security.invoker.{ApiException, ApiInvoker}
import org.kaloz.cdc.security.model.{BlockedUsersResponse, ErrorResponse, PostVerifyAdvertRequest}

import scalaz._

class ApiWrapper(baseUri: String) {
  private val clientId: String = "contract_client"
  private val advertApi = new AdvertApi(baseUri + "/api")
  private val usersApi = new UsersApi(baseUri + "/api")

  def verifyAdvert(body: PostVerifyAdvertRequest): ErrorResponse \/ Unit =
    withFallBackToError(advertApi.verifyAdvert(clientId, body))

  def blockUser(userId: String): ErrorResponse \/ Unit =
    withFallBackToError(usersApi.blockUser(clientId, userId))

  def unblockUser(userId: String): ErrorResponse \/ Unit =
    withFallBackToError(usersApi.unblockUser(clientId, userId))

  def blockedUsers(): ErrorResponse \/ BlockedUsersResponse =
    withFallBackToError(usersApi.blockedUsers(clientId).get)

  private def withFallBackToError[T](call: => T): ErrorResponse \/ T =
    \/.fromTryCatchThrowable[T, Throwable] {
      call
    }.leftMap {
      case ex: ApiException if ex.code == HttpStatus.SC_INTERNAL_SERVER_ERROR => ErrorResponse("internal-server-error", ex.getMessage)
      case ex: ApiException => ApiInvoker.deserialize(ex.getMessage, "", classOf[ErrorResponse]).asInstanceOf[ErrorResponse]
      case ex: Exception => ErrorResponse("internal-server-error", ex.getMessage)
    }
}
