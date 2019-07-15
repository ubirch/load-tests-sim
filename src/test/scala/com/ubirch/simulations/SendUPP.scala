package com.ubirch.simulations

import com.ubirch.models.{ AbstractUbirchClient, DataGeneration, ReadFileControl }
import com.ubirch.util.{ DataGenerationFileConfigs, EnvConfigs, Helpers, WithJsonFormats }
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.JsonMethods._

import scala.language.postfixOps

trait WithProtocol extends EnvConfigs {
  val httpProtocol = http.baseUrl("https://niomon." + ENV + ".ubirch.com")
}

object SendUPP extends DataGenerationFileConfigs with WithJsonFormats {

  val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  val loadData = {
    ReadFileControl(path, directory, fileName, ext).read { l =>

      val dataGeneration = parse(l).extractOpt[DataGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
      val auth: String = Helpers.encodedAuth(dataGeneration.deviceCredentials)
      data += Map("UPP" -> dataGeneration.upp, "auth" -> auth)

    }
  }

  def createBody(session: Session) = {
    val value = session("UPP").as[String]
    AbstractUbirchClient.toBytesFromHex(value)
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

  val scn = scenario("Device Message (UPP)")
    .feed(data.toIndexedSeq.queue)
    .exec(send)

}
