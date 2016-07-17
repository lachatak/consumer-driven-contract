package org.kaloz.pact.provider.scalatest

import org.scalatest.Tag

object Tags {

  /**
    * Pact tests are annotated with this tag by default
    */
  object PactTest extends Tag("org.kaloz.pact.provider.scalatest.Tags.PactTest")

}
