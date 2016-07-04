package org.kaloz.cdc.consumer


import com.gumtree.greeting.handler.DefaultApi
import com.gumtree.greeting.invoker.ApiInvoker
import com.itv.scalapact.ScalaPactForger.{GET, forgePact, interaction}
import org.apache.http.HttpStatus
import org.scalatest.{FunSpec, Matchers}

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
            .willRespondWith(HttpStatus.SC_OK, Map("Content-Type" -> "text/plain"), "Hello Consumer1!")
        )
        .runConsumerTest { mockConfig =>
          val apiInvoker = new ApiInvoker
          val defaultApi = new DefaultApi(mockConfig.baseUrl, apiInvoker)
          defaultApi.greeting("Consumer1") should equal(Some("Hello Consumer1!"))
        }
    }
  }
}
