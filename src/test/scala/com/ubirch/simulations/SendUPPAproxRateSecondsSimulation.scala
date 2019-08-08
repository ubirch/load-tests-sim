package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPAproxRateSecondsSimulation
  extends Simulation
  with SendUPP
  with Protocols
  with ConfigBase {

  val numberOfUsers: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.duringValue")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  // TODO make configurable
  setUp(sendScenario(devices).inject(constantUsersPerSec(1000).during(15 minutes)))
    .throttle(
      reachRps(1000) in (1 minute),
      holdFor(2 minute),
      reachRps(2000) in (1 minute),
      holdFor(2 minutes)
    ).protocols(niomonProtocol)

}

