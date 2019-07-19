package com.ubirch.simulations

import com.ubirch.util._
import io.gatling.core.Predef._

import scala.language.postfixOps

class SendUPPAtOnceUserSimulation
  extends Simulation
  with SendUPP
  with Protocols
  with ConfigBase {

  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  setUp(sendScenario(devices).inject(atOnceUsers(numberOfUsers))).protocols(niomonProtocol)

}
