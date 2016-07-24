package org.kaloz.cdc.advert

import com.itv.scalapact.ScalaPactForger.{GET, forgePact, interaction}
import org.apache.http.HttpStatus
import org.scalatest.{FunSpec, Matchers}

import scalaj.http.Http

class AdvertConsumerPactSpec extends FunSpec with Matchers {

  describe("Advert Service Consumer Test") {

    it("Should be able to create a contract for a time service") {

      val endPoint = "/time"

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("provide the time")
            .given("time is set to 01-07-2016")
            .uponReceiving(GET, endPoint)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "text/plain"), "01-07-2016")
        )
        .runConsumerTest { mockConfig =>

          val response = Http(mockConfig.baseUrl + endPoint).asString

          response.code should equal(HttpStatus.SC_OK)
          response.header("Content-Type") should equal(Some("text/plain"))
          response.body should equal("01-07-2016")

        }
    }

    it("Should be able to create a contract for a time service2") {

      val endPoint = "/time"

      forgePact
        .between("advert-service")
        .and("security-service")
        .addInteraction(
          interaction
            .description("provide the time")
            .given("time is set to 01-07-2017")
            .uponReceiving(GET, endPoint)
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "text/plain"), "01-07-2017")
        )
        .runConsumerTest { mockConfig =>

          val response = Http(mockConfig.baseUrl + endPoint).asString

          response.code should equal(HttpStatus.SC_OK)
          response.header("Content-Type") should equal(Some("text/plain"))
          response.body should equal("01-07-2017")

        }

    }
  }
}
