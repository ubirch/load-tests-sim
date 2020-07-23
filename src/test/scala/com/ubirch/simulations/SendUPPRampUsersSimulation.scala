package com.ubirch.simulations

import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPRampUsersSimulation extends Simulation with WithScenarios {

  val numberOfUsers: Int = conf.getInt("sendUPPRampUsersSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("sendUPPRampUsersSimulation.duringValue")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(
    sendScenarioWithFileData(devices).inject(
      rampUsers(numberOfUsers)
        .during(duringValue seconds)
    )
  ).protocols(niomonProtocol)

}
