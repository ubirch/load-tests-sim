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

  val rps: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rps")
  val rampUp: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rampup")
  val length: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.length")

  setUp(
    sendScenario(devices)
      .inject(
        constantUsersPerSec(numberOfUsers) //The number of users here can be thought of as the number of req/s.
          //if set to 14, it will send 14 messages per second
          .during(duringValue seconds)
      )
      .throttle(
        reachRps(rps) in (rampUp seconds),
        holdFor(length seconds),
      )

  ).protocols(niomonProtocol)

}

