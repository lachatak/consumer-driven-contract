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
    def complying(consumer: Consumer): Pact = Pact(provider, consumer)

    case class Pact(provider: String, consumer: Consumer) {
      def pacts(using: testing): RestartHandler = RestartHandler(provider, consumer, using.starter)
    }

  }

  case class RestartHandler(provider: String, consumer: Consumer, starter: ServerStarter) {
    def withoutRestart() = verifyPact(provider, consumer, starter)

    def withRestart() = verifyPact(provider, consumer, starter, true)
  }

  trait Consumer {
    val filter: ConsumerInfo => Boolean
  }

  case object all extends Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => true
  }

  case class testing(starter: ServerStarter)

  implicit def strToProvider(provider: String) = Provider(provider)

  implicit def strToConsumer(consumer: String) = new Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => consumerInfo.getName == consumer
  }

  private def verifyPact(provider: String, consumer: Consumer, starter: ServerStarter, restartServer: Boolean = false): Unit = {

    val verifier = new ProviderVerifier
    val consumerInfos = ProviderUtils.loadPactFiles(new model.Provider(provider), new File(getClass.getClassLoader.getResource("pacts-dependents").toURI)).asInstanceOf[java.util.List[ConsumerInfo]]
    consumerInfos.filter(consumer.filter).map { consumerInfo =>
      val pact = verifier.loadPactFileForConsumer(consumerInfo).asInstanceOf[Pact]
      pact.getInteractions.map(_.asInstanceOf[RequestResponseInteraction]).foreach { interaction =>
        val description = new StringBuilder(interaction.getDescription)
        if (interaction.getProviderState != null) description.append(s" given ${interaction.getProviderState}")
        provider should description.toString() in {
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
}

trait ServerStarter {

  def url: String

  def startServer(): Unit

  def initState(state: String)

  def isRunning(): Boolean

  def stopServer(): Unit
}

abstract class CompactPactProviderRestartSpec(provider: String) extends ProviderSpec with ServerStarter {
  provider complying all pacts testing(this) withRestart
}

abstract class CompactPactProviderStateFulSpec(provider: String) extends ProviderSpec with ServerStarter {
  provider complying all pacts testing(this) withoutRestart
}

