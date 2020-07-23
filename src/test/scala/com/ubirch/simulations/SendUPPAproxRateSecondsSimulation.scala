package com.ubirch.simulations

import com.ubirch.DeviceGenerator
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

  logger.info(s"injecting $constantUsers users during $during minutes")
  logger.info(s"execution plan ($stepCount steps):")
  for (i <- 0 until stepCount) {
    logger.info(f"[$i%02d] reach ${steps(i).head} rps in ${steps(i)(1)} min, hold for ${steps(i)(2)} min")
  }

  setUp(sendScenarioWithFileData(devices).inject(constantUsersPerSec(constantUsers).during(during minutes)))
    .throttle(
      (0 until stepCount).toList.flatMap { i =>
        List(
          reachRps(steps(i).head) in (steps(i)(1) minute),
          holdFor(steps(i)(2) minute)
        )
      }
    )
    .protocols(niomonProtocol)

}

class SendUPPAproxRateSecondsSimulation2
  extends Simulation
  with SendUPP
  with Protocols
  with ConfigBase {

  val rps1: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rps1")
  val rampup1: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rampup1")
  val holdfor1: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.holdfor1")

  val rps2: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rps2")
  val rampup2: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.rampup2")
  val holdfor2: Int = conf.getInt("sendUPPAproxRateSecondsSimulation.holdfor2")

  val devices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)

  val constantUsers: Int = math.max(rps1, rps2)
  val during: Int = rampup1 + holdfor1 + rampup2 + holdfor2

  // TODO make configurable
  setUp(sendScenarioWithFileData(devices).inject(constantUsersPerSec(constantUsers).during(during minutes)))
    .throttle(
      reachRps(rps1) in (rampup1 minute),
      holdFor(holdfor1 minute),
      reachRps(rps2) in (rampup2 minute),
      holdFor(holdfor2 minutes)
    ).protocols(niomonProtocol)

}

class SendUPPAproxRateSecondsSimulation3
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

  val onlyTheseDevices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)
  val devices = DeviceGenerator.loadDevices(onlyTheseDevices)
  val continuous = new Continuous(devices)

  logger.info("Found {} devices", continuous.length)

  val constantUsers: Int = (0 until stepCount).toList.map(i => steps(i).head).max
  val during: Int = (0 until stepCount).toList.flatMap(i => List(steps(i)(1), steps(i)(2))).sum

  logger.info(s"injecting $constantUsers users during $during minutes")
  logger.info(s"execution plan ($stepCount steps):")

  for (i <- 0 until stepCount) {
    logger.info(f"[$i%02d] reach ${steps(i).head} rps in ${steps(i)(1)} min, hold for ${steps(i)(2)} min")
  }

  setUp(
    sendScenarioWithContinuousData(continuous)
      .inject(constantUsersPerSec(constantUsers).during(during minutes))
  )
    .throttle(
      (0 until stepCount).toList.flatMap { i =>
        List(
          reachRps(steps(i).head) in (steps(i)(1) minute),
          holdFor(steps(i)(2) minute)
        )
      }
    )
    .protocols(niomonProtocol)

}

