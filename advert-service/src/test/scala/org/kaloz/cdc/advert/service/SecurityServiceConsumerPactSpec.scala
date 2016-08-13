package org.kaloz.cdc.advert.service

import com.itv.scalapact.ScalaPactForger.{POST, forgePact, interaction}
import org.apache.http.HttpStatus
import org.kaloz.cdc.security.invoker.ScalaJsonUtil._
import org.scalatest.{FunSpec, Matchers}
import org.kaloz.cdc.security.model.{ErrorResponse, PostVerifyAdvertRequest}

import scalaz.{-\/, \/-}


class SecurityServiceConsumerPactSpec extends FunSpec with Matchers {

  describe("Advert Service") {

    it("should be able to post valid advert for verification") {

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to verify advert")
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

    it("should be able to post advert with blocked user for verification") {

      val response: ErrorResponse = ErrorResponse("user_blocked", "1 is blocked!")

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to verify advert")
            .given("the user with id '1' is blocked")
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

    it("should be able to post advert with invalid content for verification") {

      val response: ErrorResponse = ErrorResponse("rejected_ad_description", "Ad contains invalid data")

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to verify advert")
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

    it("should be able to post advert for verification when there is internal server error") {

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to verify advert")
            .given("there is a server error")
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
