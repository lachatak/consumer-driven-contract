package org.kaloz.cdc.mobileclient

import com.itv.scalapact.ScalaPactForger.{POST, forgePact, interaction}
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.kaloz.cdc.advert.invoker.ScalaJsonUtil.getJsonMapper
import org.kaloz.cdc.advert.model.{ErrorResponse, PostAdvertRequest, PostAdvertRequestAd, PostAdvertResponse}
import org.kaloz.cdc.mobileclient.api.ApiWrapper
import org.scalatest.{FunSpec, Matchers}

import scalaz._

class AdvertServiceConsumerPactSpec extends FunSpec with Matchers {

  val dateTime = DateTime.parse("17-07-16 14.53.12", DateTimeFormat.forPattern("dd-MM-yy HH.mm.ss"))

  describe("Mobile Client") {

    it("should be able to post a new valid advert") {

      val response: PostAdvertResponse = PostAdvertResponse("1", dateTime.toDate)

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("the advert is valid")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "mobile_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(response))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(\/-(response))
        }
    }

    it("should be able to post a new advert when the user is blocked") {

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("the user with id '1' is blocked")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "mobile_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("1", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_BAD_REQUEST, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(ErrorResponse("user_blocked", "1 is blocked!")))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("1", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("user_blocked", "1 is blocked!")))
        }
    }

    it("should be able to post a new advert when the content is invalid") {

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("the ad contains invalid content")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "mobile_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_BAD_REQUEST, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(ErrorResponse("rejected_ad_content", "Ad contains rejected content!")))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("rejected_ad_content", "Ad contains rejected content!")))
        }
    }

    it("should be able to post a new advert when there is an internal server error") {

      forgePact
        .between("mobile-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("be able to process a new advert post")
            .given("there is a server error")
            .uponReceiving(POST,
              "/api/adverts",
              None,
              Map("client_id" -> "mobile_client"),
              Some(getJsonMapper.writeValueAsString(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc")))),
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.postAdvert(PostAdvertRequest("userId", PostAdvertRequestAd(1, 100, "desc"))) should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }
  }
}
