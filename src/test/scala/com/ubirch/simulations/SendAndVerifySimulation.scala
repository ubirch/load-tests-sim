package com.ubirch.simulations

import com.ubirch.DataGenerator
import io.gatling.core.Predef._

class SendAndVerifySimulation extends Simulation with WithScenarios {

  val numberOfUsers: Int = conf.getInt("sendAndVerifySimulation.numberOfUsers")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  DataGenerator.main(Array.empty)

  setUp(sendAndVerifyScenario(devices).inject(atOnceUsers(numberOfUsers))).protocols(niomonProtocol)

}
