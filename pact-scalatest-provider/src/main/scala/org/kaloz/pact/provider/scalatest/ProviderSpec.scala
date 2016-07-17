package org.kaloz.pact.provider.scalatest

import java.io.File
import java.util.concurrent.Executors

import au.com.dius.pact.model
import au.com.dius.pact.model.{FullResponseMatch, RequestResponseInteraction, ResponseMatching, Pact => PactForConsumer}
import au.com.dius.pact.provider.sbtsupport.HttpClient
import au.com.dius.pact.provider.{ConsumerInfo, ProviderUtils, ProviderVerifier}
import org.kaloz.pact.provider.scalatest.Tags.PactTest
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait ProviderSpec extends FlatSpec with Matchers {

  /**
    * Verifies pacts with a given configuration.
    * Every item will be run as a standalone {@link org.scalatest.FlatSpec}
    *
    * @param verificationConfig
    */
  def verify(verificationConfig: VerificationConfig): Unit = {

    import verificationConfig.pact._
    import verificationConfig.serverConfig._

    val verifier = new ProviderVerifier
    ProviderUtils.loadPactFiles(new model.Provider(provider), new File(getClass.getClassLoader.getResource("pacts-dependents").toURI)).asInstanceOf[java.util.List[ConsumerInfo]]
      .filter(consumer.filter)
      .flatMap(c => verifier.loadPactFileForConsumer(c).asInstanceOf[PactForConsumer].getInteractions.map(i => (c.getName, i.asInstanceOf[RequestResponseInteraction])))
      .foreach { case (consumerName, interaction) =>
        val description = new StringBuilder(s"${interaction.getDescription} for '$consumerName'")
        if (interaction.getProviderState != null) description.append(s" given ${interaction.getProviderState}")
        provider should description.toString() taggedAs PactTest in {
          if (!serverStarter.isRunning()) serverStarter.startServer()
          serverStarter.initState(interaction.getProviderState)
          implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
          val request = interaction.getRequest.copy
          request.setPath(s"${serverStarter.url}${interaction.getRequest.getPath}")
          val actualResponseFuture = HttpClient.run(request)
          val actualResponse = Await.result(actualResponseFuture, 5 seconds)
          if (restartServer) serverStarter.stopServer()
          ResponseMatching.matchRules(interaction.getResponse, actualResponse) shouldBe (FullResponseMatch)
        }
      }
  }
}

abstract class PactProviderRestartDslSpec(provider: String) extends ProviderSpec with ProviderDsl with ServerStarter {
  verify(provider complying all pacts testing(this) withRestart)
}

abstract class PactProviderStateFulDslSpec(provider: String) extends ProviderSpec with ProviderDsl with ServerStarter {
  verify(provider complying all pacts testing(this) withoutRestart)
}

