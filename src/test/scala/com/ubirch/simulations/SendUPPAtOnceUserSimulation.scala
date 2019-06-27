package com.ubirch.simulations

import java.util.{ Base64, UUID }

import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl, SimpleProtocolImpl }
import com.ubirch.util.{ EnvConfigs, DataFileConfigs }
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import org.apache.http.auth.UsernamePasswordCredentials
import scala.concurrent.duration._
import scala.language.postfixOps

class SendUPPAtOnceUserSimulation extends Simulation with EnvConfigs with DataFileConfigs {

  val credentials = new UsernamePasswordCredentials(authUser, authPass)
  val auth: String = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)
  val numberOfUsers: Int = conf.getInt("sendUPPAtOnceUserSimulation.numberOfUsers")

  val httpProtocol = http
    .baseUrl("https://niomon." + ENV + ".ubirch.com")
    .authorizationHeader("Basic " + auth)

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

class SendUPPRampUsersSimulation extends Simulation with EnvConfigs with DataFileConfigs {

  val credentials = new UsernamePasswordCredentials(authUser, authPass)
  val auth: String = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)
  val numberOfUsers: Int = conf.getInt("sendUPPRampUsersSimulation.numberOfUsers")
  val duringValue: Int = conf.getInt("sendUPPRampUsersSimulation.duringValue")

  val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  ReadFileControl(path, directory, fileName, ext).read { l =>
    l.split(";").toList.headOption.foreach { x =>
      data += Map("UPP" -> x)
    }
  }

  val httpProtocol = http
    .baseUrl("https://niomon." + ENV + ".ubirch.com")
    .authorizationHeader("Basic " + auth)

  def createBody(session: Session) = {
    val value = session("UPP").as[String]
    AbstractUbirchClient.toBytesFromHex(value)
  }

  val scn = scenario("SendUPPSimulation")
    .feed(data.toIndexedSeq.queue)
    .exec(
      http("Send UPP data")
        .post("/")
        .body(ByteArrayBody(createBody))
    )

  setUp(
    scn.inject(
      rampUsers(numberOfUsers)
        .during(duringValue seconds)
    )
  ).protocols(httpProtocol)

}
