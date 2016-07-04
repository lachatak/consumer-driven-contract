package org.kaloz.cdc.time

import java.util.concurrent.Executors

import au.com.dius.pact.model.{FullResponseMatch, PactReader, RequestResponsePact, ResponseMatching}
import au.com.dius.pact.provider.sbtsupport.HttpClient
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait ProviderSpec extends FlatSpec with Matchers {

  case class Provider(provider: String) {
    def complying(consumer: String) = Pact(provider, consumer)

    case class Pact(provider: String, consumer: String) {
      def pacts(using: using): Unit = verifyPact(provider, consumer, using.starter)
    }

  }

  case class using(starter: Starter)

  implicit def strToProvider(provider: String) = Provider(provider)

  private def verifyPact(provider: String, consumer: String, starter: Starter): Unit = {
    val pact = PactReader.loadPact(getClass.getClassLoader.getResourceAsStream(s"pacts-dependents/$consumer-$provider.json")).asInstanceOf[RequestResponsePact]
    JavaConversions.asScalaBuffer(pact.getInteractions).map { interaction =>
      val description = new StringBuilder(interaction.getDescription)
      if (interaction.getProviderState != null) description.append(s" given ${interaction.getProviderState}")
      val test: String => Unit = { url =>
        implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
        val request = interaction.getRequest.copy
        request.setPath(s"$url${interaction.getRequest.getPath}")
        val actualResponseFuture = HttpClient.run(request)
        val actualResponse = Await.result(actualResponseFuture, 5 seconds)
        ResponseMatching.matchRules(interaction.getResponse, actualResponse) shouldBe (FullResponseMatch)
      }
      provider should description.toString() in {
        test(starter.start(interaction.getProviderState))
        starter.tearDown()
      }
    }
  }
}

trait Starter {

  def start(state: String): String

  def tearDown(): Unit
}

