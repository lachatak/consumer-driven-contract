package org.kaloz.cdc.web

import com.itv.scalapact.ScalaPactForger.{GET, forgePact, interaction}
import org.apache.http.HttpStatus
import org.scalatest.{FunSpec, Matchers}

import scalaj.http.Http

class WebClientConsumerPactSpec extends FunSpec with Matchers {

  //TODO update to use the real contract
  describe("Web Client Consumer Test") {

    it("Should be able to create a contract for a greeting service") {

      val endPoint = "/greeting"

      forgePact
        .between("web-client")
        .and("advert-service")
        .addInteraction(
          interaction
            .description("a simple get for client2 greeting")
            .uponReceiving(GET, endPoint, Some("client=Consumer2"))
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "text/plain"), "Hello Consumer2!")
        )
        .runConsumerTest { mockConfig =>

          val response = Http(mockConfig.baseUrl + endPoint).param("client", "Consumer2").asString

          response.code should equal(HttpStatus.SC_OK)
          response.header("Content-Type") should equal(Some("text/plain"))
          response.body should equal("Hello Consumer2!")

        }

    }
  }
}
