package com.ubirch.simulations

import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class VerifyUPPRampUsersSimulation extends Simulation with WithScenarios {

  val numberOfUsers: Int = conf.getInt("verifyUPPRampUsersSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("verifyUPPRampUsersSimulation.duringValue")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(
    verifyScenario(devices).inject(
      rampUsers(numberOfUsers)
        .during(duringValue seconds)
    )
  ).protocols(verificationProtocol)

}
