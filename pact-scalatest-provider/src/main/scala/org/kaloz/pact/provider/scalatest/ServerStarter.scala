package org.kaloz.pact.provider.scalatest

trait ServerStarter {

  def url: String

  def startServer(): Unit

  def initState(state: String)

  def isRunning(): Boolean

  def stopServer(): Unit
}
