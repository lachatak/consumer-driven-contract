package org.kaloz.cdc.consumer

import com.itv.scalapact.ScalaPactForger.{GET, forgePact, interaction}
import org.apache.http.HttpStatus
import org.scalatest.{FunSpec, Matchers}

import scalaj.http.Http

class Consumer1PactSpec extends FunSpec with Matchers {

  describe("Consumer 1 CDC test") {

    it("Should be able to create a contract for a greeting service") {

      val endPoint = "/greeting"

      forgePact
        .between("consumer1")
        .and("greeting")
        .addInteraction(
          interaction
            .description("a simple get for client1 greeting")
            .given("a condition is given")
            .uponReceiving(GET, endPoint, Some("client=Consumer1"))
            .willRespondWith(HttpStatus.SC_OK, "Hello Consumer1!")
        )
        .runConsumerTest { mockConfig =>

          val response = Http(mockConfig.baseUrl + endPoint).param("client", "Consumer1").asString

          response.code should equal(HttpStatus.SC_OK)
          response.body should equal("Hello Consumer1!")

        }

    }
  }
}
