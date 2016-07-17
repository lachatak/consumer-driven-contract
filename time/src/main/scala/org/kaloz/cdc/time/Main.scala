package org.kaloz.cdc.time

import java.net.URL

import org.http4s.MediaType.`text/plain`
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder

object Main extends App {

  new ServerImpl().startServer()

}

class ServerImpl {

  private var server: Server = _

  var date: String = "01-07-2016"

  def startServer(): Unit = {

    val service = HttpService {
      case GET -> Root / "time" =>
        Ok(date).withContentType(Some(`Content-Type`(`text/plain`)))
    }

    val server = BlazeBuilder.bindHttp(8080)
      .mountService(service, "/")
      .run

    this.server = server
  }

  def stopServer() = server.shutdownNow()


  def url(): URL = new URL(s"http://${server.address.getHostName}:${server.address.getPort}")


}
