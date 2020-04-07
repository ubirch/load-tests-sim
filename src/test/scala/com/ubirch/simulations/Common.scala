package com.ubirch.simulations

import java.nio.charset.StandardCharsets
import java.util.Base64

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.DeviceGenerator
import com.ubirch.models.{ DataGeneration, ReadFileControl }
import com.ubirch.util.{ DataGenerationFileConfigs, Helpers, WithJsonFormats }
import io.gatling.core.Predef._
import io.gatling.core.feeder.SourceFeederBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import org.json4s.jackson.JsonMethods._

trait Common extends DataGenerationFileConfigs with WithJsonFormats with LazyLogging {

  val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  def dataReadType: String = conf.getString("dataReadType")
  val consoleRegistration: Boolean = conf.getBoolean("deviceGenerator.consoleRegistration")

  def prepareData(suffixes: List[String]): SourceFeederBuilder[String] = {
    val data = loadData(suffixes).toIndexedSeq
    dataReadType match {
      case "queue" => data.queue
      case "random" => data.random
      case "shuffle" => data.shuffle
      case _ => data.queue
    }

  }

  def loadData(suffixes: List[String]) = {
    ReadFileControl(path, directory, fileName, suffixes, ext).read { l =>

      lazy val dataGeneration = parse(l).extractOpt[DataGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
      lazy val auth: String = Helpers.encodedAuth(dataGeneration.deviceCredentials)
      data += Map(
        "UPP" -> dataGeneration.upp,
        "HASH" -> dataGeneration.hash,
        "password" -> Base64.getEncoder.encodeToString(DeviceGenerator.getPassword(dataGeneration.deviceCredentials).getBytes(StandardCharsets.UTF_8)),
        "hardware_id" -> dataGeneration.UUID.toString,
        "auth" -> (if (consoleRegistration) "" else auth)
      )

    }

    println("Data total: " + data.size)
    logger.info("Data total: " + data.size)
    if (data.isEmpty) throw new Exception("No Data Found. Please generate the needed data.")

    data
  }

  def getScenario(scenarioName: String, suffixes: List[String], exec: HttpRequestBuilder) = {
    scenario(scenarioName)
      .feed(prepareData(suffixes).circular)
      .exec(exec)
  }

}
