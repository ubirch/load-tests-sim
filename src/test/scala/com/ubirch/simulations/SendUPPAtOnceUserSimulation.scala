package com.ubirch.simulations

import com.ubirch.models.{ AbstractUbirchClient, DataGeneration, ReadFileControl }
import com.ubirch.util._
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import org.json4s.jackson.JsonMethods._

import scala.language.postfixOps

class SendUPPAtOnceUserSimulation2
  extends Simulation
  with WithProtocol
  with EnvConfigs
  with DataGenerationFileConfigs
  with WithJsonFormats {

  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")

  val execsBuff = scala.collection.mutable.ListBuffer.empty[ChainBuilder]

  ReadFileControl(path, directory, fileName, ext).read { l =>

    val dataGeneration = parse(l).extractOpt[DataGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
    val auth: String = Helpers.encodedAuth(dataGeneration.deviceCredentials)

    execsBuff += exec(http("Send data " + dataGeneration.UUID)
      .post("/")
      .header("Authorization", "Basic " + auth)
      .body(ByteArrayBody(AbstractUbirchClient.toBytesFromHex(dataGeneration.upp))))
      .pause(1)

  }

  val scn = scenario("SendUPPSimulation").exec(execsBuff)

  setUp(
    scn.inject(
      atOnceUsers(numberOfUsers)
    )
  ).protocols(httpProtocol)

}

class SendUPPAtOnceUserSimulation
  extends Simulation
  with WithProtocol
  with ConfigBase {

  import SendUPP._

  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")

  setUp(scn.inject(atOnceUsers(numberOfUsers))).protocols(httpProtocol)

}
