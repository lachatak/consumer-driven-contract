package org.kaloz.cdc.time

class TimeSpec extends ProviderSpec {


  val starter: Starter = new Starter {

    var server: ServerImpl = _

    override def start(state: String): String = {
      server = new ServerImpl()
      server.startServer()
      if (state.contains("01-07-2017")) server.date = "01-07-2017"
      server.url()
    }

    override def tearDown(): Unit = {
      server.stopServer()
    }

  }

  "time" complying "greeting" pacts using(starter)

}
