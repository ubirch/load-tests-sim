package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPRampUsersSimulation
  extends Simulation
  with SendUPP
  with Protocols
  with ConfigBase {

  val numberOfUsers: Int = conf.getInt("sendUPPRampUsersSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("sendUPPRampUsersSimulation.duringValue")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(
    sendScenario(devices).inject(
      rampUsers(numberOfUsers)
        .during(duringValue seconds)
    )
  ).protocols(niomonProtocol)

}
