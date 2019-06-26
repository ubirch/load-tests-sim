package com.ubirch.simulations

import java.util.Base64

import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl, SimpleProtocolImpl }
import com.ubirch.util.{ EnvConfigs, FileConfigs }
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.http.auth.UsernamePasswordCredentials

class SendUPPSimulation extends Simulation with EnvConfigs with FileConfigs { // 3

  val buffer = scala.collection.mutable.ListBuffer.empty[String]

  ReadFileControl(path, directory, fileName, ext).read { l =>
    l.split(";").toList.headOption.foreach(x => buffer += x)
  }

  val credentials = new UsernamePasswordCredentials(authUser, authPass)

  val protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey)

  val auth: String = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)

  val httpProtocol = http
    .baseUrl("https://niomon." + ENV + ".ubirch.com")
    .authorizationHeader("Basic " + auth)

  val execs = buffer.map { x =>
    exec(http("send data " + x).post("/").body(ByteArrayBody(AbstractUbirchClient.toBytesFromHex(x))))
  }

  val scn = scenario("SendUPPSimulation").exec(execs)

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
