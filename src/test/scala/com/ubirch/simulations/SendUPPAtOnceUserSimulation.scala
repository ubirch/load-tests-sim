package com.ubirch.simulations

import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl }
import com.ubirch.util.{ DataGenerationFileConfigs, EnvConfigs }
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.language.postfixOps

class SendUPPAtOnceUserSimulation
  extends Simulation
  with WithProtocol
  with EnvConfigs
  with DataGenerationFileConfigs {

  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")

  val execsBuff = scala.collection.mutable.ListBuffer.empty[ChainBuilder]

  ReadFileControl(path, directory, fileName, ext).read { l =>
    l.split(";").toList.headOption.foreach { x =>
      execsBuff += exec(http("send data " + x)
        .post("/")
        .body(ByteArrayBody(AbstractUbirchClient.toBytesFromHex(x))))
        .pause(1)
    }
  }

  val scn = scenario("SendUPPSimulation").exec(execsBuff)

  setUp(
    scn.inject(
      atOnceUsers(numberOfUsers)
    )
  ).protocols(httpProtocol)

}

