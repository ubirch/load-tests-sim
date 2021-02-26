package com.ubirch.simulations

import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class VerifyUPPAproxRateSecondsSimulationContinuous extends Simulation with WithScenarios {

  val stepConfs = conf.getConfigList("sendUPPAproxRateSecondsSimulation.steps")
  val stepCount = stepConfs.size()

  val steps = (0 until stepCount).toList.map { i =>
    List(
      stepConfs.get(i).getInt("rps"),
      stepConfs.get(i).getInt("rampup"),
      stepConfs.get(i).getInt("holdfor")
    )
  }

  val onlyTheseDevices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)
  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  val constantUsers: Int = (0 until stepCount).toList.map(i => steps(i).head).max
  val during: Int = (0 until stepCount).toList.flatMap(i => List(steps(i)(1), steps(i)(2))).sum

  logger.info(s"injecting $constantUsers users during $during minutes")
  logger.info(s"execution plan ($stepCount steps):")

  for (i <- 0 until stepCount) {
    logger.info(f"[$i%02d] reach ${steps(i).head} rps in ${steps(i)(1)} min, hold for ${steps(i)(2)} min")
  }

  setUp(
    verifyScenario(devices)
      .inject(constantUsersPerSec(constantUsers.toDouble).during(during minutes))
  )
    .throttle(
      (0 until stepCount).toList.flatMap { i =>
        List(
          reachRps(steps(i).head) in (steps(i)(1) minute),
          holdFor(steps(i)(2) minute)
        )
      }
    )
    .protocols(verificationProtocol)

}

