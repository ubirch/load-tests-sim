package com.ubirch.simulations

import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl }
import com.ubirch.util.{ ConfigBase, DataGenerationFileConfigs, EnvConfigs }
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.language.postfixOps

class SendUPPAtOnceUserSimulation2
  extends Simulation
  with WithProtocol
  with EnvConfigs
  with DataGenerationFileConfigs {

  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")

  val execsBuff = scala.collection.mutable.ListBuffer.empty[ChainBuilder]

  ReadFileControl(path, directory, fileName, ext).read { l =>
    l.split(";").toList match {
      case List(uuid, deviceCredentials, upp, _) =>

        val auth: String = SendUPP.encodedAuth(deviceCredentials)

        execsBuff += exec(http("Send data " + uuid)
          .post("/")
          .header("Authorization", "Basic " + auth)
          .body(ByteArrayBody(AbstractUbirchClient.toBytesFromHex(upp))))
          .pause(1)

      case _ => throw new Exception("No Data is malformed")

    }
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
