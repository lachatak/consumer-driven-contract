package org.kaloz.cdc.time

import java.net.URL

import org.kaloz.pact.provider.scalatest.{PactProviderRestartDslSpec, PactProviderStatefulDslSpec}

class TimeStatefulSpec extends PactProviderStatefulDslSpec("time") {

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

class TimeRestartSpec extends PactProviderRestartDslSpec("time") {

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
