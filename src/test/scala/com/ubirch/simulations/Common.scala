package com.ubirch.simulations

import java.nio.charset.StandardCharsets
import java.util.Base64

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.{ DataGeneration, DeviceGeneration, PayloadGenerator, ReadFileControl }
import com.ubirch.util._
import com.ubirch.{ DataGenerator, DeviceGenerator, KeyRegistration }
import io.gatling.core.Predef._
import io.gatling.core.feeder.SourceFeederBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import org.json4s.jackson.JsonMethods._

import scala.util.Try

trait Common extends WithJsonFormats with ConfigBase with LazyLogging {

  lazy val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  lazy val dataReadType: String = conf.getString("dataReadType")
  lazy val consoleRegistration: Boolean = conf.getBoolean("deviceGenerator.consoleRegistration")

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
    ReadFileControl(
      DataGenerationFileConfigs.path,
      DataGenerationFileConfigs.directory,
      DataGenerationFileConfigs.fileName,
      suffixes,
      DataGenerationFileConfigs.ext
    ).read { l =>

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

  class Continuous(deviceGenerations: Seq[DeviceGeneration]) {

    val length = deviceGenerations.length

    val generators = deviceGenerations.map { dataGeneration =>
      val clientKey = KeyRegistration.getKey(dataGeneration.privateKey)
      val payloadGenerator = DataGenerator.payloadGenerator(dataGeneration.UUID, clientKey, EnvConfigs.serverUUID, EnvConfigs.serverKey)
      (dataGeneration, payloadGenerator)
    }

    def random: (DeviceGeneration, PayloadGenerator) = generators(scala.util.Random.nextInt(length))

    val feeder: Iterator[Map[String, String]] = Iterator.continually {

      val (dataGeneration, payloadGenerator) = random
      lazy val auth: String = Helpers.encodedAuth(dataGeneration.deviceCredentials)

      val (_, upp, hash) = payloadGenerator.getOneAsString

      val password = Try(
        Base64.getEncoder
          .encodeToString(DeviceGenerator.getPassword(dataGeneration.deviceCredentials).getBytes(StandardCharsets.UTF_8))
      ).get

      Map(
        "UPP" -> upp,
        "HASH" -> hash,
        "password" -> password,
        "hardware_id" -> dataGeneration.UUID.toString,
        "auth" -> (if (consoleRegistration) "" else auth)
      )
    }
  }

  def getScenarioWithFileData(scenarioName: String, suffixes: List[String], exec: HttpRequestBuilder) = {
    scenario(scenarioName)
      .feed(prepareData(suffixes).circular)
      .exec(exec)
  }

  def getScenarioWithContinuousData(scenarioName: String, continuous: Continuous, exec: HttpRequestBuilder) = {
    scenario(scenarioName)
      .feed(continuous.feeder)
      .exec(exec)
  }

}
