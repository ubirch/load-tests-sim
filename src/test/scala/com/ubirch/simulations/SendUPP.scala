package com.ubirch.simulations

import java.util.Base64

import com.ubirch.DeviceGenerator
import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl }
import com.ubirch.util.{ DataGenerationFileConfigs, EnvConfigs }
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.http.auth.UsernamePasswordCredentials

import scala.language.postfixOps

trait WithProtocol extends EnvConfigs {
  val httpProtocol = http.baseUrl("https://niomon." + ENV + ".ubirch.com")
}

object SendUPP extends DataGenerationFileConfigs {

  val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  val loadData = {
    ReadFileControl(path, directory, fileName, ext).read { l =>

      l.split(";").toList match {
        case List(_, deviceCredentials, upp, hash) =>
          val auth: String = SendUPP.encodedAuth(deviceCredentials)
          data += Map("UPP" -> upp, "auth" -> auth)

        case Nil => throw new Exception("Data is malformed")
      }

    }
  }

  def createBody(session: Session) = {
    val value = session("UPP").as[String]
    AbstractUbirchClient.toBytesFromHex(value)
  }

  def encodedAuth(deviceCredentials: String) = {
    val (username, password) = DeviceGenerator.getDeviceCredentials(deviceCredentials)
    val credentials = new UsernamePasswordCredentials(username, password)
    val auth: String = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)
    auth
  }

  def authHeader(session: Session) = {
    val auth = session("auth").as[String]
    "Basic " + auth
  }

  val send = {
    http("Send UPP data")
      .post("/")
      .header("Authorization", session => authHeader(session))
      .body(ByteArrayBody(createBody))
  }

  val scn = scenario("SendUPPSimulation")
    .feed(data.toIndexedSeq.queue)
    .exec(send)

}
