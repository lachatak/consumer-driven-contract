package org.kaloz.cdc.advert.service

import com.itv.scalapact.ScalaPactForger.{POST, forgePact, interaction}
import org.apache.http.HttpStatus
import org.kaloz.cdc.security.invoker.ScalaJsonUtil._
import org.scalatest.{FunSpec, Matchers}
import org.kaloz.cdc.security.model.{ErrorResponse, PostVerifyAdvertRequest}

import scalaz.{-\/, \/-}


class SecurityServiceConsumerPactSpec extends FunSpec with Matchers {

  describe("Security Service Consumer Test") {

    it("should be able to verify advert with valid data") {

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("Adverts service posts a new advert to verify")
            .given("the advert is valid")
            .uponReceiving(POST,
              "/api/security/advert",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostVerifyAdvertRequest("1", "desc"))),
              None)
            .willRespondWith(HttpStatus.SC_OK)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.verifyAdvert(PostVerifyAdvertRequest("1", "desc")) should equal(\/-(()))
        }
    }

    it("should be able to verify advert with blocked user") {

      val response: ErrorResponse = ErrorResponse("user_blocked", "Used is blocked")

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("Adverts service client posts a new advert to verify")
            .given("the user is blocked")
            .uponReceiving(POST,
              "/api/security/advert",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostVerifyAdvertRequest("1", "desc"))),
              None)
            .willRespondWith(HttpStatus.SC_FORBIDDEN, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.verifyAdvert(PostVerifyAdvertRequest("1", "desc")) should equal(-\/(response))
        }
    }

    it("should be able to verify advert with invalid data") {

      val response: ErrorResponse = ErrorResponse("rejected_ad_description", "Ad contains invalid data")

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("Adverts service posts a new advert to verify")
            .given("ad contains invalid data")
            .uponReceiving(POST,
              "/api/security/advert",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostVerifyAdvertRequest("1", "desc"))),
              None)
            .willRespondWith(HttpStatus.SC_FORBIDDEN, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.verifyAdvert(PostVerifyAdvertRequest("1", "desc")) should equal(-\/(response))
        }
    }

    it("should be able to verify advert with internal server error") {

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("Adverts service posts a new advert to verify")
            .given("the there is a server error")
            .uponReceiving(POST,
              "/api/security/advert",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostVerifyAdvertRequest("1", "desc"))),
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.verifyAdvert(PostVerifyAdvertRequest("1", "desc")) should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }

  }
}
