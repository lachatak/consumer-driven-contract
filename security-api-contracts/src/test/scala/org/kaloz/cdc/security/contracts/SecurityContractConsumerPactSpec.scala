package org.kaloz.cdc.security.contracts

import com.itv.scalapact.ScalaPactForger.{DELETE, GET, POST, forgePact, interaction}
import org.apache.http.HttpStatus
import org.kaloz.cdc.security.invoker.ScalaJsonUtil._
import org.kaloz.cdc.security.model.{BlockedUsersResponse, ErrorResponse, PostVerifyAdvertRequest}
import org.scalatest.{FunSpec, Matchers}

import scala.org.kaloz.cdc.security.contracts.ApiWrapper
import scalaz.{-\/, \/-}

class SecurityContractConsumerPactSpec extends FunSpec with Matchers {

  describe("Security API Contract Implementor") {

    it("should be able to verify advert with valid data") {

      forgePact
        .between("security-api-contract")
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

    it("should be able to verify advert with blocked user") {

      val response: ErrorResponse = ErrorResponse("user_blocked", "Used is blocked")

      forgePact
        .between("security-api-contract")
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

    it("should be able to verify advert when the ad content is invalid") {

      val response: ErrorResponse = ErrorResponse("rejected_ad_description", "Ad contains invalid data")

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to verify advert")
            .given("the ad content is invalid")
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
        .between("security-api-contract")
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

    it("should be able to get list of blocked users") {

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to get list of blocked users")
            .given("there are blocked users")
            .uponReceiving(GET,
              "/api/security/users",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "application/json"), getJsonMapper.writeValueAsString(BlockedUsersResponse(List("1", "2"))))
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.blockedUsers() should equal(\/-(BlockedUsersResponse(List("1", "2"))))
        }
    }

    it("should be able to get list of blocked users with internal server error") {

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to get list of blocked users")
            .given("there is a server error")
            .uponReceiving(GET,
              "/api/security/users",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.blockedUsers() should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }

    it("should be able to block user") {

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to block user")
            .uponReceiving(POST,
              "/api/security/users",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_OK)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.blockUser("1") should equal(\/-(()))
        }
    }

    it("should be able to block users with internal server error") {

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to block users")
            .given("there is a server error")
            .uponReceiving(POST,
              "/api/security/users/1",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.blockUser("1") should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }

    it("should be able to unblock user") {

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to unblock user")
            .given("blocked user exists with id '1'")
            .uponReceiving(DELETE,
              "/api/security/users/1",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_OK)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.unblockUser("1") should equal(\/-(()))
        }
    }

    it("should be able to unblock users with internal server error") {

      forgePact
        .between("security-api-contract")
        .and("security-service")
        .addInteraction(
          interaction
            .description("be able to unblock user")
            .given("there is a server error")
            .uponReceiving(DELETE,
              "/api/security/users/1",
              None,
              Map("client_id" -> "contract_client"),
              None,
              None)
            .willRespondWith(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        )
        .runConsumerTest { mockConfig =>
          val advertApi = new ApiWrapper(mockConfig.baseUrl)
          advertApi.unblockUser("1") should equal(-\/(ErrorResponse("internal-server-error", "")))
        }
    }
  }
}
