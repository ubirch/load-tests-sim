package com.ubirch.simulations

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.utils.Utils
import com.ubirch.models._
import com.ubirch.util.{ EnvConfigs, _ }
import com.ubirch.{ DataGenerator, DeviceGenerator, KeyRegistration }
import io.gatling.core.Predef._
import io.gatling.core.feeder.SourceFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.{ HttpHeaderNames, HttpHeaderValues, http, _ }
import io.gatling.http.protocol.HttpProtocolBuilder
import io.gatling.http.request.builder.HttpRequestBuilder
import org.json4s.JString
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

import scala.concurrent.duration._
import scala.language.postfixOps

trait WithScenarios extends ConfigBase with WithJsonFormats with LazyLogging {

  private val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  private val dataReadType: String = conf.getString("dataReadType")
  private val consoleRegistration: Boolean = conf.getBoolean("deviceGenerator.consoleRegistration")
  private val maxConnections: Int = conf.getInt("maxConnectionsForSendingUPPSimulation")

  private val sendHashUrl: String = "https://api.certify." + EnvConfigs.ENV + ".ubirch.com/api/v1/anchor-test"
  //private val sendHashUrl: String = "http://localhost:8081/api/v1/anchor-test"
  private val sendUPPUrl: String = "https://niomon." + EnvConfigs.ENV + ".ubirch.com"
  private val verifyUrl: String = "https://verify." + EnvConfigs.ENV + ".ubirch.com/api/upp/verify"
  //private val verifyWithAnchorUrl: String = verifyUrl + "/anchor"
  private val verifyWithRecordUrl: String = verifyUrl + "/record"

  val gen = new SecureRandom()

  private val sendUPP: HttpRequestBuilder = {
    http("Send UPP data")
      .post("/")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationOctetStream)
      .header("X-Ubirch-Hardware-Id", session => session("hardware_id").as[String])
      .header("X-Ubirch-Auth-Type", "ubirch")
      .header("X-Ubirch-Credential", session => session("password").as[String])
      .header("Authorization", session => "Basic " + session("auth").as[String])
      .body(ByteArrayBody(session => AbstractUbirchClient.toBytesFromHex(session("UPP").as[String])))
  }

  private val sendHash: HttpRequestBuilder = {
    http("Send HASH data")
      .post(sendHashUrl)
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.TextPlain)
      .header("X-Test-Token", _ => {
        val token = Token(
          "TEST_IS_A_TEST",
          JString("THIS IS A TEST"),
          "test-driver",
          "Mr Test Driver",
          "driver@test.com",
          //TODO: This needs to adjusted to fit testing data after file loading is ready
          List(Symbol("vaccination-center-altoetting"), Symbol("certification-vaccination"))
        //List(Symbol(session("hardware_id").as[String]))
        )
        write(token)
      })
      .body(ByteArrayBody { _ =>
        Base64.getEncoder.encode(Utils.secureRandomBytes(32))
      })
  }

  private val verify: HttpRequestBuilder = {
    http("Verify UPP data")
      .post(verifyWithRecordUrl)
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
      .body(StringBody { session =>
        val hash = session("HASH").as[String]
        //println("HASH::: " + hash)
        hash
      }).transformResponse {
        (_, r) =>
          //println("STATUS::: " + r.status)
          r
      }.check(bodyString.find.notNull)

  }

  val niomonProtocol: HttpProtocolBuilder = http
    .baseUrl(sendUPPUrl)
    .shareConnections
    .maxConnectionsPerHost(maxConnections)

  val certifyProtocol: HttpProtocolBuilder = http
    .baseUrl(sendHashUrl)
    .shareConnections
    .maxConnectionsPerHost(maxConnections)

  val verificationProtocol: HttpProtocolBuilder = http.baseUrl(verifyUrl)

  private def prepareData(suffixes: List[String]): SourceFeederBuilder[String] = {
    val data = loadData(suffixes).toIndexedSeq
    dataReadType match {
      case "queue" => data.queue
      case "random" => data.random
      case "shuffle" => data.shuffle
      case _ => data.queue
    }
  }

  private def loadData(suffixes: List[String]) = {
    ReadFileControl(
      DataGenerationFileConfigs.path,
      DataGenerationFileConfigs.directory,
      DataGenerationFileConfigs.fileName,
      suffixes,
      DataGenerationFileConfigs.ext
    ).read { l =>

        lazy val dataGeneration = parse(l).extractOpt[DataGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
        lazy val auth: String = Helpers.encodedAuth(dataGeneration.deviceCredentials)

        val passwordAsBytes = DeviceGenerator.getPassword(dataGeneration.deviceCredentials).getBytes(StandardCharsets.UTF_8)
        val passwordAsBase64 = Base64.getEncoder.encodeToString(passwordAsBytes)

        data += Map(
          "UPP" -> dataGeneration.upp,
          "HASH" -> dataGeneration.hash,
          "password" -> passwordAsBase64,
          "hardware_id" -> dataGeneration.UUID.toString,
          "auth" -> (if (consoleRegistration) "" else auth)
        )

      }

    println("Data total: " + data.size)
    logger.info("Data total: " + data.size)
    if (data.isEmpty) throw new Exception("No Data Found. Please generate the needed data.")

    data
  }

  def sendScenarioWithFileData(suffixes: List[String]): ScenarioBuilder = {
    getScenarioWithFileData("Device Message (UPPs)", suffixes, sendUPP)
  }

  def sendScenarioWithContinuousData(continuous: Continuous): ScenarioBuilder = {
    getScenarioWithContinuousData("Device Message (Continuous UPPs)", continuous, sendUPP)
  }

  def sendHashScenarioWithContinuousData(continuous: Continuous): ScenarioBuilder = {
    getScenarioWithContinuousData("Device HASH Message (Continuous UPPs)", continuous, sendHash)
  }

  def sendAndVerifyScenario(suffixes: List[String]): ScenarioBuilder = {
    getScenarioWithFileData("Device Message (UPPs)", suffixes, sendUPP)
      .pause(10 seconds)
      .exec(verify)
  }

  def getScenarioWithFileData(scenarioName: String, suffixes: List[String], exec: HttpRequestBuilder): ScenarioBuilder = {
    scenario(scenarioName)
      .feed(prepareData(suffixes).circular)
      .exec(exec)
  }

  def getScenarioWithContinuousData(scenarioName: String, continuous: Continuous, exec: HttpRequestBuilder): ScenarioBuilder = {
    scenario(scenarioName)
      .feed(continuous.feeder)
      .exec(exec)
  }

  def verifyScenario(suffixes: List[String]): ScenarioBuilder = {
    getScenarioWithFileData("Device Verification (UPP)", suffixes, verify)
  }

  class Continuous(deviceGenerations: Seq[DeviceGeneration]) {

    val length: Int = deviceGenerations.length

    val generators: Seq[(DeviceGeneration, PayloadGenerator)] = deviceGenerations.map { dataGeneration =>
      val clientKey = KeyRegistration.getKey(dataGeneration.privateKey)
      val payloadGenerator = DataGenerator.payloadGenerator(dataGeneration.UUID, clientKey, EnvConfigs.serverUUID, EnvConfigs.serverKey)
      (dataGeneration, payloadGenerator)
    }
    val feeder: Iterator[Map[String, String]] = Iterator.continually {

      val (deviceGeneration, payloadGenerator) = random
      lazy val auth: String = Helpers.encodedAuth(deviceGeneration.deviceCredentials)

      val (_, upp, hash) = payloadGenerator.getOneAsString

      val passwordAsBytes = DeviceGenerator
        .getPassword(deviceGeneration.deviceCredentials)
        .getBytes(StandardCharsets.UTF_8)
      val passwordAsBase64 = Base64.getEncoder.encodeToString(passwordAsBytes)

      DataGeneration(deviceGeneration.UUID, deviceGeneration.deviceCredentials, upp, hash)

      // Writes data to file after creating it.
      //val dataGeneration = DataGeneration(deviceGeneration.UUID, deviceGeneration.deviceCredentials, upp, hash)
      //      WriteFileControl(
      //        DataGenerationFileConfigs.numberOfMessagesPerFile,
      //        DataGenerationFileConfigs.path,
      //        DataGenerationFileConfigs.directory,
      //        DataGenerationFileConfigs.fileName,
      //        "Verification_",
      //        DataGenerationFileConfigs.ext
      //      ).secured { w =>
      //          val dataToStore = compact(Extraction.decompose(dataGeneration))
      //          w.append(dataToStore)
      //        }

      Map(
        "UPP" -> upp,
        "HASH" -> hash,
        "password" -> passwordAsBase64,
        "hardware_id" -> deviceGeneration.UUID.toString,
        "auth" -> (if (consoleRegistration) "" else auth)
      )

    }

    def random: (DeviceGeneration, PayloadGenerator) = generators(scala.util.Random.nextInt(length))
  }

}
