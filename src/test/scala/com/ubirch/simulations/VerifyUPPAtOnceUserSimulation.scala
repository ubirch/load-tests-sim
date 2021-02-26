package com.ubirch.simulations

import io.gatling.core.Predef._

class VerifyUPPAtOnceUserSimulation extends Simulation with WithScenarios {

  val numberOfUsers: Int = conf.getInt("verifyUPPAtOnceUserSimulation.numberOfUsers")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(verifyScenario(devices).inject(atOnceUsers(numberOfUsers))).protocols(verificationProtocol)

}
