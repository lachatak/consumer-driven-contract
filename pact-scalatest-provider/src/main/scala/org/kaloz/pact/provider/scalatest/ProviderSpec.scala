package org.kaloz.pact.provider.scalatest

import java.io.File
import java.util.concurrent.Executors

import au.com.dius.pact.model
import au.com.dius.pact.model.{FullResponseMatch, Pact, RequestResponseInteraction, ResponseMatching}
import au.com.dius.pact.provider.sbtsupport.HttpClient
import au.com.dius.pact.provider.{ConsumerInfo, ProviderUtils, ProviderVerifier}
import org.kaloz.pact.provider.scalatest.Tags.PactTest
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait ProviderSpec extends FlatSpec with Matchers {

  case class Provider(provider: String) {
    def complying(consumer: Consumer): PactBetween = PactBetween(provider, consumer)

    case class PactBetween(provider: String, consumer: Consumer) {
      def pacts(using: testing): RestartHandler = RestartHandler(provider, consumer, using.starter)
    }

    case class RestartHandler(provider: String, consumer: Consumer, starter: ServerStarter) {
      def withoutRestart() = VerificationConfig(provider, consumer, starter)

      def withRestart() = VerificationConfig(provider, consumer, starter, true)
    }

  }

  case class VerificationConfig(provider: String, consumer: Consumer, starter: ServerStarter, restartServer: Boolean = false)

  trait Consumer {
    val filter: ConsumerInfo => Boolean
  }

  /**
    * Allows every pacts to run against the producer
    */
  case object all extends Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => true
  }

  /**
    * Defines the server which will be used for testing the pacts
    *
    * @param starter
    */
  case class testing(starter: ServerStarter)

  /**
    * Support string provider in the DSL
    *
    * @param provider
    * @return
    */
  implicit def strToProvider(provider: String) = Provider(provider)

  /**
    * Allows just the matching consumer pacts to run against the producer
    *
    * @param consumer
    * @return
    */
  implicit def strToConsumer(consumer: String) = new Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => consumerInfo.getName == consumer
  }

  /**
    * Verifies pacts with a given configuration.
    * Every item will be run as a standalone {@link org.scalatest.FlatSpec}
    *
    * @param verificationConfig
    */
  def verify(verificationConfig: VerificationConfig): Unit = {

    import verificationConfig._

    val verifier = new ProviderVerifier
    ProviderUtils.loadPactFiles(new model.Provider(verificationConfig.provider), new File(getClass.getClassLoader.getResource("pacts-dependents").toURI)).asInstanceOf[java.util.List[ConsumerInfo]]
      .filter(consumer.filter)
      .flatMap(c => verifier.loadPactFileForConsumer(c).asInstanceOf[Pact].getInteractions.map(i => (c.getName, i.asInstanceOf[RequestResponseInteraction])))
      .foreach { case (consumerName, interaction) =>
        val description = new StringBuilder(s"${interaction.getDescription} for '$consumerName'")
        if (interaction.getProviderState != null) description.append(s" given ${interaction.getProviderState}")
        provider should description.toString() taggedAs PactTest in {
          if (!starter.isRunning()) starter.startServer()
          starter.initState(interaction.getProviderState)
          implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
          val request = interaction.getRequest.copy
          request.setPath(s"${starter.url}${interaction.getRequest.getPath}")
          val actualResponseFuture = HttpClient.run(request)
          val actualResponse = Await.result(actualResponseFuture, 5 seconds)
          if (restartServer) starter.stopServer()
          ResponseMatching.matchRules(interaction.getResponse, actualResponse) shouldBe (FullResponseMatch)
        }
      }
  }
}

abstract class CompactPactProviderRestartSpec(provider: String) extends ProviderSpec with ServerStarter {
  verify(provider complying all pacts testing(this) withRestart)
}

abstract class CompactPactProviderStateFulSpec(provider: String) extends ProviderSpec with ServerStarter {
  verify(provider complying all pacts testing(this) withoutRestart)
}

