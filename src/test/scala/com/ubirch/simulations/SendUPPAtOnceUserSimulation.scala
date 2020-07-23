package com.ubirch.simulations

import io.gatling.core.Predef._

import scala.language.postfixOps

class SendUPPAtOnceUserSimulation extends Simulation with WithScenarios {

  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(sendScenarioWithFileData(devices).inject(atOnceUsers(numberOfUsers))).protocols(niomonProtocol)

}
