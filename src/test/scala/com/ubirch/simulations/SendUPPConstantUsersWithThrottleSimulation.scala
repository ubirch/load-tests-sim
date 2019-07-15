package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPConstantUsersWithThrottleSimulation
  extends Simulation
  with WithProtocol
  with ConfigBase {

  import SendUPP._

  val numberOfUsers: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.duringValue")
  val reachRpsV: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.reachRps")
  val inV: Int = conf.getInt("sendUPPConstantUsersWithThrottleSimulation.in")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(scn(devices).inject(constantUsersPerSec(numberOfUsers).during(duringValue seconds)))
    .throttle(reachRps(reachRpsV) in (inV seconds))
    .protocols(httpProtocol)

}
