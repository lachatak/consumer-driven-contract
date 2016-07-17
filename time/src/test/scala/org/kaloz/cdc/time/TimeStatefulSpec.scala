package org.kaloz.cdc.time

import org.kaloz.pact.provider.scalatest.{PactProviderRestartDslSpec, PactProviderStatefulDslSpec}

class TimeStatefulSpec extends PactProviderStatefulDslSpec("time") {

  var server: ServerImpl = _

  var url: String = _

  override def startServer(): Unit = {
    println("Starting server...")
    server = new ServerImpl()
    server.startServer()
    url = server.url()
  }

  override def initState(state: String): Unit = if (state.contains("01-07-2017")) server.date = "01-07-2017" else server.date = "01-07-2016"

  override def isRunning(): Boolean = server != null

  override def stopServer(): Unit = {
    println("Stopping server...")
    server.stopServer()
    server = null
  }

}

class TimeRestartSpec extends PactProviderRestartDslSpec("time") {

  var server: ServerImpl = _

  var url: String = _

  override def startServer(): Unit = {
    println("Starting server...")
    server = new ServerImpl()
    server.startServer()
    url = server.url()
  }

  override def initState(state: String): Unit = if (state.contains("01-07-2017")) server.date = "01-07-2017" else server.date = "01-07-2016"

  override def isRunning(): Boolean = server != null

  override def stopServer(): Unit = {
    println("Stopping server...")
    server.stopServer()
    server = null
  }

}
