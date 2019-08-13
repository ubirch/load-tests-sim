package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPAproxRateSecondsSimulation
  extends Simulation
    with SendUPP
    with Protocols
    with ConfigBase {

  val stepConfs = conf.getConfigList("sendUPPAproxRateSecondsSimulation.steps")
  val stepCount = stepConfs.size()

  val steps = (0 until stepCount).toList.map { i =>
    List(
      stepConfs.get(i).getInt("rps"),
      stepConfs.get(i).getInt("rampup"),
      stepConfs.get(i).getInt("holdfor")
    )
  }

  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  val constantUsers: Int = (0 until stepCount).toList.map(i => steps(i).head).max
  val during: Int = (0 until stepCount).toList.flatMap(i => List(steps(i)(1), steps(i)(2))).sum


  // TODO make configurable
  setUp(sendScenario(devices).inject(constantUsersPerSec(constantUsers).during(during minutes)))
    .throttle(
      (0 until stepCount).toList.flatMap { i =>
        List(
          reachRps(steps(i)(0)) in (steps(i)(1) minute),
          holdFor(steps(i)(2) minute)
        )
      }
    )
    .protocols(niomonProtocol)

}

