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

  val rps1: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rps1")
  val rampup1: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rampup1")
  val holdfor1: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.holdfor1")

  val rps2: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rps2")
  val rampup2: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rampup2")
  val holdfor2: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.holdfor2")

  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  val constantUsers: Int = math.max(rps1, rps2)
  val during: Int = rampup1 + holdfor1 + rampup2 + holdfor2

  // TODO make configurable
  setUp(sendScenario(devices).inject(constantUsersPerSec(constantUsers).during(during minutes)))
    .throttle(
      reachRps(rps1) in (rampup1 minute),
      holdFor(holdfor1 minute),
      reachRps(rps2) in (rampup2 minute),
      holdFor(holdfor2 minutes)
    ).protocols(niomonProtocol)

}

