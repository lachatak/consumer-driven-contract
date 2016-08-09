package org.kaloz.cdc.web

import com.itv.scalapact.ScalaPactForger.{GET, POST, forgePact, interaction}
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.kaloz.cdc.advert.invoker.ScalaJsonUtil._
import org.kaloz.cdc.advert.model.{ErrorResponse, PostAdvertRequest, PostAdvertRequestAd, PostAdvertResponse}
import org.kaloz.cdc.mobileclient.api.ApiWrapper
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}

class WebClientConsumerPactSpec extends FunSpec with Matchers {

  val dateTime = DateTime.parse("17-07-16 14.53.12", DateTimeFormat.forPattern("dd-MM-yy HH.mm.ss"))

  describe("Web Client Consumer") {

    it("should be able to post a new valid advert") {

      val response: PostAdvertResponse = PostAdvertResponse("1", dateTime.toDate)

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("Mobile client posts a new advert")
            .given("the advert is valid")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "web_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(\/-(response))
        }
    }

    it("should be able to handle a new post ad with a blocked user") {

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("Mobile client posts a new advert")
            .given("the user_id is blocked")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "web_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_BAD_REQUEST, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(ErrorResponse("user_blocked", "userId is blocked!")))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("user_blocked", "userId is blocked!")))
        }
    }

    it("should be able to handle a new post ad with an invalid content") {

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("Mobile client posts a new advert")
            .given("the ad contains invalid content")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "web_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_BAD_REQUEST, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(ErrorResponse("rejected_ad_content", "Ad contains rejected content!")))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("rejected_ad_content", "Ad contains rejected content!")))
        }
    }

    it("should be able to handle a new post ad with internal server error") {

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("Mobile client posts a new advert")
            .given("the server is down")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "web_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }

    it("should be able to get all the adverts") {

      val response = AdvertListResponse(List(UserAdvertDetails("userId",
        List(AdvertDetails("1", dateTime.toDate, "active", 1, "desc", 100), AdvertDetails("2", dateTime.plusDays(2).toDate, "active", 1, "desc", 100))
      )))

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("Advert API contract client queries all the ads")
            .given("there are ads available in the service")
            .uponReceiving(GET,
              "/api/adverts",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.adverts() should equal(\/-(response))
        }
    }

    it("should be able to get all the adverts with internal server error") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("Advert API contract client queries all the ads")
            .given("the there is a server error")
            .uponReceiving(GET,
              "/api/adverts",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.adverts() should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }
  }
}
