package org.kaloz.cdc.security

import java.net.URL

import au.com.dius.pact.provider.scalatest.{PactProviderStatefulDslSpec, ServerStarter}

class SecurityProviderStatefulPactSpec extends PactProviderStatefulDslSpec("security-service") {

  lazy val serverStarter: ServerStarter = new ServerStarter {

    var server: ServerImpl = _

    override def startServer(): URL = {
      println("Starting server...")
      server = new ServerImpl()
      server.startServer()
      server.url()
    }

    override def initState(state: String): Unit = if (state.contains("01-07-2017")) server.date = "01-07-2017" else server.date = "01-07-2016"

    override def stopServer(): Unit = {
      println("Stopping server...")
      server.stopServer()
    }

  }
}
