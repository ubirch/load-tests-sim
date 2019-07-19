package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.language.postfixOps

class VerifyUPPAtOnceUserSimulation
  extends Simulation
  with VerifyUPP
  with Protocols
  with ConfigBase {

  val numberOfUsers: Int = conf.getInt("verifyUPPAtOnceUserSimulation.numberOfUsers")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(verifyScenario(devices).inject(atOnceUsers(numberOfUsers))).protocols(verificationProtocol)

}
