package org.kaloz.pact.provider.scalatest

import au.com.dius.pact.provider.ConsumerInfo

trait ProviderDsl {

  case class Provider(provider: String) {
    def complying(consumer: Consumer): PactBetween = PactBetween(provider, consumer)
  }

  case class PactBetween(provider: String, consumer: Consumer) {
    def pacts(using: testing): ServerHandler = ServerHandler(this, using.starter)
  }

  case class ServerHandler(pactBetween: PactBetween, serverStarter: ServerStarter) {
    def withoutRestart() = VerificationConfig(Pact(pactBetween.provider, pactBetween.consumer), ServerConfig(serverStarter))

    def withRestart() = VerificationConfig(Pact(pactBetween.provider, pactBetween.consumer), ServerConfig(serverStarter, true))
  }

  /**
    * Support string provider in the DSL
    *
    * @param provider
    * @return
    */
  implicit def strToProvider(provider: String) = Provider(provider)

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

}

object ProviderDsl extends ProviderDsl
