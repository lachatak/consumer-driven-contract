package org.kaloz.pact.provider

import au.com.dius.pact.provider.ConsumerInfo

package object scalatest {

  trait Consumer {
    val filter: ConsumerInfo => Boolean
  }

  /**
    * Allows just the matching consumer pacts to run against the producer
    *
    * @param consumer
    * @return
    */
  implicit def strToConsumer(consumer: String) = new Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => consumerInfo.getName == consumer
  }

  case class Pact(provider: String, consumer: Consumer)

  case class ServerConfig(serverStarter: ServerStarter, restartServer: Boolean = false)

  case class VerificationConfig(pact: Pact, serverConfig: ServerConfig)

}
