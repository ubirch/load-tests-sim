package com.ubirch.simulations

import com.ubirch.util.{ ConfigBase, EnvConfigs }
import io.gatling.core.Predef._
import io.gatling.http.Predef._

trait Protocols extends ConfigBase {

  val maxConnections = conf.getInt("maxConnectionsForSendingUPPSimulation")

  lazy val niomonProtocol = http
    .baseUrl("https://niomon." + EnvConfigs.ENV + ".ubirch.com")
    .shareConnections
    .maxConnectionsPerHost(maxConnections)

  val verifyUrl = "https://verify." + EnvConfigs.ENV + ".ubirch.com/api/upp/verify"

  lazy val verificationProtocol = http.baseUrl(verifyUrl)

}
