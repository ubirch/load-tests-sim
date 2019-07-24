package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPConstantUsersWithThrottleSimulation
  extends Simulation
  with SendUPP
  with Protocols
  with ConfigBase {

  // *****
  // Temporarily disabled so we can run some quick tests. //
  // *****
  //val numberOfUsers: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.numberOfUsers")
  //val duringValue: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.duringValue")
  //val reachRpsV: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.reachRps")
  //val inV: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.in")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(
    sendScenario(devices)
      .inject(
        constantUsersPerSec(200)
          .during(60 minutes)
      )
  ).throttle(reachRps(200) in (10 seconds), holdFor(55))
    .maxDuration(60 seconds)
    .protocols(niomonProtocol)

}
