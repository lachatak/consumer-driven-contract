package org.kaloz.pact.provider.scalatest

import java.net.URL

trait ServerStarter {

  def startServer(): URL

  def initState(state: String)

  def stopServer(): Unit
}
