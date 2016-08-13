package org.kaloz.cdc.advert.contracts


import com.itv.scalapact.ScalaPactForger.{GET, POST, forgePact, interaction}
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.kaloz.cdc.advert.invoker.ScalaJsonUtil._
import org.kaloz.cdc.advert.model.{AdvertDetails, AdvertDetailsResponse, AdvertListResponse, ErrorResponse, PostAdvertRequest, PostAdvertRequestAd, PostAdvertResponse, UserAdvertDetails}
import org.scalatest.{FunSpec, Matchers}

import scala.org.kaloz.cdc.advert.contracts.ApiWrapper
import scalaz.{-\/, \/-}

class AdvertContractConsumerPactSpec extends FunSpec with Matchers {

  val dateTime = DateTime.parse("17-07-16 14.53.12", DateTimeFormat.forPattern("dd-MM-yy HH.mm.ss"))

  describe("Advert API Contract Implementor") {

    it("should be able to process a new valid advert post") {

      val response: PostAdvertResponse = PostAdvertResponse("1", dateTime.toDate)

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("the advert is valid")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("1", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("1", PostAdvertRequestAd(1, 100, "desc"))) should equal(\/-(response))
        }
    }

    it("should be able to process a new advert post when the user is blocked") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("the user with id '1' is blocked")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("1", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_BAD_REQUEST, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(ErrorResponse("user_blocked", "1 is blocked!")))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("1", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("user_blocked", "1 is blocked!")))
        }
    }

    it("should be able to process a new advert post when the content is invalid") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("the ad contains invalid content")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_BAD_REQUEST, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(ErrorResponse("rejected_ad_content", "Ad contains rejected content!")))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("rejected_ad_content", "Ad contains rejected content!")))
        }
    }

    it("should be able to process a new advert post when there is an internal server error") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("there is a server error")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "contract_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }

    it("should be able to process get all adverts request") {

      val response = AdvertListResponse(List(UserAdvertDetails("userId",
        List(AdvertDetails("1", dateTime.toDate, "active", 1, "desc", 100), AdvertDetails("2", dateTime.plusDays(2).toDate, "active", 1, "desc", 100))
      )))

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process get all adverts request")
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

    it("should be able to process get all adverts request when there is an internal server error") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process get all adverts request")
            .given("there is a server error")
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

    it("should be able to process get advert by id") {

      val response = AdvertDetailsResponse("userId", "1", dateTime.toDate, "active", 1, "desc", 100)

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process get advert by id")
            .given("there is an ad with id '1'")
            .uponReceiving(GET,
              "/api/adverts/1",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.findAdvertById("1") should equal(\/-(Some(response)))
        }
    }

    it("should be able to process get advert by id when the advert is not available") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process get advert by id")
            .given("there is no ad with id '1'")
            .uponReceiving(GET,
              "/api/adverts/1",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_NOT_FOUND)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.findAdvertById("1") should equal(\/-(None))
        }
    }

    it("should be able to process get advert by id when there is an internal server error") {

      forgePact
        .between("advert-api-contract")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process get advert by id")
            .given("there is a server error")
            .uponReceiving(GET,
              "/api/adverts/1",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.findAdvertById("1") should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }
  }
}
