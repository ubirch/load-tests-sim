package com.ubirch.simulations

import com.ubirch.DataGenerator
import com.ubirch.util._
import io.gatling.core.Predef._

import scala.language.postfixOps

class SendAndVerifySimulation
  extends Simulation
  with SendAndVerifyUPP
  with Protocols
  with ConfigBase {

  val numberOfUsers: Int = conf.getInt("sendAndVerifySimulation.numberOfUsers")
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  DataGenerator.main(Array.empty)

  setUp(sendAndVerifyScenario(devices).inject(atOnceUsers(numberOfUsers))).protocols(niomonProtocol)

}
