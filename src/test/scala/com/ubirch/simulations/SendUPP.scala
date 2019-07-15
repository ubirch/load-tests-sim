package com.ubirch.simulations

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.{ AbstractUbirchClient, DataGeneration, ReadFileControl }
import com.ubirch.util.{ DataGenerationFileConfigs, EnvConfigs, Helpers, WithJsonFormats }
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.JsonMethods._

import scala.language.postfixOps

trait WithProtocol extends EnvConfigs {
  val httpProtocol = http.baseUrl("https://niomon." + ENV + ".ubirch.com")
}

object SendUPP extends DataGenerationFileConfigs with WithJsonFormats with LazyLogging {

  val dataReadType: String = conf.getString("dataReadType")

  val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  def loadData(suffixes: List[String]) = {
    ReadFileControl(path, directory, fileName, suffixes, ext).read { l =>

      val dataGeneration = parse(l).extractOpt[DataGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
      val auth: String = Helpers.encodedAuth(dataGeneration.deviceCredentials)
      data += Map("UPP" -> dataGeneration.upp, "auth" -> auth)

    }

    println("Data total: " + data.size)
    logger.info("Data total: " + data.size)
    if (data.isEmpty) throw new Exception("No Data Found. Please generate the needed data.")

    data
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

  def prepareData(suffixes: List[String]) = {
    val data = loadData(suffixes).toIndexedSeq
    dataReadType match {
      case "queue" => data.queue
      case "random" => data.random
      case "shuffle" => data.shuffle
      case _ => data.queue
    }

  }

  def scn(suffixes: List[String]) = scenario("Device Message (UPP)")
    .feed(prepareData(suffixes))
    .exec(send)

}
