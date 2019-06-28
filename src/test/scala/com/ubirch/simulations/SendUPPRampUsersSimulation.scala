package com.ubirch.simulations

import com.ubirch.util.ConfigBase
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPRampUsersSimulation
  extends Simulation
  with WithProtocol
  with ConfigBase {

  import SendUPP._

  val numberOfUsers: Int = conf.getInt("sendUPPRampUsersSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("sendUPPRampUsersSimulation.duringValue")

  setUp(
    scn.inject(
      rampUsers(numberOfUsers)
        .during(duringValue seconds)
    )
  ).protocols(httpProtocol)

}
