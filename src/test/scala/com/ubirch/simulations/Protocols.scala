package com.ubirch.simulations

import com.ubirch.util.EnvConfigs
import io.gatling.core.Predef._
import io.gatling.http.Predef._

trait Protocols extends EnvConfigs {

  val maxConnections = conf.getInt("maxConnectionsForSendingUPPSimulation")

  lazy val niomonProtocol = http
    .baseUrl("https://niomon." + ENV + ".ubirch.com")
    .shareConnections
    .maxConnectionsPerHost(maxConnections)

  val verifyUrl = "https://verify." + ENV + ".ubirch.com/api/verify"

  lazy val verificationProtocol = http.baseUrl(verifyUrl)

}
