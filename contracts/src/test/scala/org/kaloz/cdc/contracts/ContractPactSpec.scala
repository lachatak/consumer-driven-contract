package org.kaloz.cdc.contracts

import com.itv.scalapact.ScalaPactForger.{GET, forgePact, interaction}
import org.apache.http.HttpStatus
import org.scalatest.{FunSpec, Matchers}

import scalaj.http.Http

class ContractPactSpec extends FunSpec with Matchers {

  describe("Contract Test") {

    it("Should be able to create a contract for a greeting service") {

      val endPoint = "/greeting"

      forgePact
        .between("contract")
        .and("greeting")
        .addInteraction(
          interaction
            .description("a simple get for client1 greeting")
            .given("a condition is given")
            .uponReceiving(GET, endPoint, Some("client=Contract"))
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "text/plain"), "Hello Contract!")
        )
        .runConsumerTest { mockConfig =>
          val response = Http(mockConfig.baseUrl + endPoint).param("client", "Contract").asString

          response.code should equal(HttpStatus.SC_OK)
          response.header("Content-Type") should equal(Some("text/plain"))
          response.body should equal("Hello Contract!")
        }
    }
  }
}
