package com.ubirch.simulations

import com.ubirch.util.EnvConfigs
import io.gatling.core.Predef._
import io.gatling.http.Predef._

trait Protocols extends EnvConfigs {

  val maxConnections = conf.getInt("maxConnectionsForSendingUPPSimulation")

  lazy val niomonProtocol = http
    .baseUrl("https://niomon." + ENV + ".ubirch.com")
    .maxConnectionsPerHost(maxConnections)

  lazy val verificationProtocol = http.baseUrl("https://verify." + ENV + ".ubirch.com/api/verify")

}
