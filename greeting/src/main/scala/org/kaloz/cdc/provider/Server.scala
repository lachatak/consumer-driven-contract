package org.kaloz.cdc.provider

import org.http4s._
import org.http4s.dsl.{:?, QueryParamDecoderMatcher, _}
import org.http4s.server.blaze.BlazeBuilder

object Main extends App {

  object Client extends QueryParamDecoderMatcher[String]("client")

  val service = HttpService {
    case GET -> Root / "greeting" :? Client(client) =>
      Ok(s"Hello $client!")

    case GET -> Root / "healthcheck" =>
      Ok()
  }

  BlazeBuilder.bindHttp(8080)
    .mountService(service, "/")
    .run
    .awaitShutdown()
}
