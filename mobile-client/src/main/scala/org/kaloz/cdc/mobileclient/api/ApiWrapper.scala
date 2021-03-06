package org.kaloz.cdc.mobileclient.api

import org.apache.http.HttpStatus
import org.kaloz.cdc.advert.handler.AdvertApi
import org.kaloz.cdc.advert.invoker.{ApiException, ApiInvoker}
import org.kaloz.cdc.advert.model.{ErrorResponse, PostAdvertRequest, PostAdvertResponse}

import scalaz._

class ApiWrapper(baseUri: String) {
  private val clientId: String = "mobile_client"
  private val advertApi = new AdvertApi(baseUri + "/api")

  def postAdvert(body: PostAdvertRequest): ErrorResponse \/ PostAdvertResponse =
    withFallBackToError(advertApi.postAdvert(clientId, body).get)

  private def withFallBackToError[T](call: => T): ErrorResponse \/ T =
    \/.fromTryCatchThrowable[T, Throwable] {
      call
    }.leftMap {
      case ex: ApiException if ex.code == HttpStatus.SC_INTERNAL_SERVER_ERROR => ErrorResponse("internal-server-error", ex.getMessage)
      case ex: ApiException => ApiInvoker.deserialize(ex.getMessage, "", classOf[ErrorResponse]).asInstanceOf[ErrorResponse]
      case ex: Exception => ErrorResponse("internal-server-error", ex.getMessage)
    }
}
