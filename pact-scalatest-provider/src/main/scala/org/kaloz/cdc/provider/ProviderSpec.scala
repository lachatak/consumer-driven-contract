package org.kaloz.cdc.provider

import java.io.File
import java.util.concurrent.Executors

import au.com.dius.pact.model
import au.com.dius.pact.model.{FullResponseMatch, Pact, RequestResponseInteraction, ResponseMatching}
import au.com.dius.pact.provider.sbtsupport.HttpClient
import au.com.dius.pact.provider.{ConsumerInfo, ProviderUtils, ProviderVerifier}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait ProviderSpec extends FlatSpec with Matchers {

  case class Provider(provider: String) {
    def complying(consumer: Consumer) = Pact(provider, consumer)

    case class Pact(provider: String, consumer: Consumer) {
      def pacts(using: using): Unit = verifyPact(provider, consumer, using.starter)
    }

  }

  trait Consumer {
    val filter: ConsumerInfo => Boolean
  }

  case object all extends Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => true
  }

  case class using(starter: Starter)

  implicit def strToProvider(provider: String) = Provider(provider)

  implicit def strToConsumer(consumer: String) = new Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => consumerInfo.getName == consumer
  }

  private def verifyPact(provider: String, consumer: Consumer, starter: Starter): Unit = {

    val verifier = new ProviderVerifier
    val consumerInfos = ProviderUtils.loadPactFiles(new model.Provider(provider), new File(getClass.getClassLoader.getResource("pacts-dependents").toURI)).asInstanceOf[java.util.List[ConsumerInfo]]
    consumerInfos.filter(consumer.filter).map { consumerInfo =>
      val pact = verifier.loadPactFileForConsumer(consumerInfo).asInstanceOf[Pact]
      pact.getInteractions.map(_.asInstanceOf[RequestResponseInteraction]).foreach { interaction =>
        val description = new StringBuilder(interaction.getDescription)
        if (interaction.getProviderState != null) description.append(s" given ${interaction.getProviderState}")
        val test: String => Unit = { url =>
          implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
          val request = interaction.getRequest.copy
          request.setPath(s"$url${interaction.getRequest.getPath}")
          val actualResponseFuture = HttpClient.run(request)
          val actualResponse = Await.result(actualResponseFuture, 5 seconds)
          starter.tearDown()
          ResponseMatching.matchRules(interaction.getResponse, actualResponse) shouldBe (FullResponseMatch)
        }
        provider should description.toString() in {
          test(starter.start(interaction.getProviderState))
        }
      }
    }
  }
}

trait Starter {

  def start(state: String): String

  def tearDown(): Unit
}

abstract class CompactPactProviderSpec(provider: String) extends ProviderSpec with Starter {
  provider complying all pacts using(this)
}

