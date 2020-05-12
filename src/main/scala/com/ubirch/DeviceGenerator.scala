package com.ubirch

import java.util.{ Base64, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }
import com.ubirch.models.{ DeviceGeneration, ReadFileControl, WriteFileControl }
import com.ubirch.util.{ ConfigBase, DeviceGenerationFileConfigs, EnvConfigs, WithJsonFormats }
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{ HttpGet, HttpPost }
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.{ Extraction, JArray, JValue }

import scala.annotation.tailrec
import scala.io.StdIn.readLine

object DeviceGenerator extends ConfigBase with WithJsonFormats with LazyLogging {

  val auth = encode("devicebootstrap:" + DeviceGenerationFileConfigs.deviceBootstrap)

  val client: HttpClient = HttpClients.createMinimal()

  def loadDevices(suffixes: List[String] = Nil) = {
    ReadFileControl(
      DeviceGenerationFileConfigs.path,
      DeviceGenerationFileConfigs.directory,
      DeviceGenerationFileConfigs.fileName,
      suffixes,
      DeviceGenerationFileConfigs.ext
    )
      .read { l =>
        parse(l).extractOpt[DeviceGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
      }
  }

  def encode(data: String) = Base64.getEncoder.encodeToString(data.getBytes)

  def simpleAuthTestConsole(authToken: String) = {
    val req = new HttpGet("https://api.console.dev.ubirch.com/ubirch-web-ui/api/v1/users/accountInfo")
    req.addHeader("Authorization", "bearer " + authToken)
    req
  }

  def addDeviceInConsole(uuid: UUID, authToken: String) = {
    val req = new HttpPost("https://api.console.dev.ubirch.com/ubirch-web-ui/api/v1/devices/elephants")
    req.addHeader("Authorization", "bearer " + authToken)
    req.addHeader("Content-Type", "application/json")
    val reqBody = s"""{
    "reqType": "creation",
    "tags": "gatling",
    "prefix": "",
    "devices": [
        {
            "hwDeviceId": "${uuid.toString}",
            "description": "gatling test",
            "deviceType": "default_type",
            "apiConfig": "",
            "deviceConfig": "",
            "groups": []}]}"""
    req.setEntity(new StringEntity(reqBody))
    req
  }

  def getDeviceConfigFromConsole(uuid: UUID, authToken: String) = {
    val req = new HttpGet(s"https://api.console.dev.ubirch.com/ubirch-web-ui/api/v1/devices/${uuid.toString}")
    req.addHeader("Authorization", "bearer " + authToken)
    req.addHeader("Content-Type", "application/json")
    req
  }

  def deviceCredentialsRequest(data: String) = {
    val req = new HttpPost("https://ubirch.cumulocity.com/devicecontrol/deviceCredentials")
    req.addHeader("Content-Type", "application/vnd.com.nsn.cumulocity.deviceCredentials+json")
    req.addHeader("Accept", "application/vnd.com.nsn.cumulocity.deviceCredentials+json")
    req.addHeader("Authorization", "Basic " + auth)
    req.setEntity(new StringEntity(data))
    req
  }

  def deviceInventoryRequest(data: String, username: String, password: String) = {
    val req = new HttpPost("https://ubirch.cumulocity.com/inventory/managedObjects")
    req.addHeader("Content-Type", "application/vnd.com.nsn.cumulocity.managedObject+json")
    req.addHeader("Accept", "application/vnd.com.nsn.cumulocity.managedObject+json")
    req.addHeader("Authorization", "Basic " + encode(s"$username:$password"))
    req.setEntity(new StringEntity(data))
    req
  }

  def deviceExternalIdRequest(id: String, data: String, username: String, password: String) = {
    val req = new HttpPost(s"https://ubirch.cumulocity.com/identity/globalIds/$id/externalIds")
    req.addHeader("Content-Type", "application/vnd.com.nsn.cumulocity.externalId+json")
    req.addHeader("Accept", "application/vnd.com.nsn.cumulocity.externalId+json")
    req.addHeader("Authorization", "Basic " + encode(s"$username:$password"))
    req.setEntity(new StringEntity(data))
    req
  }

  def getDeviceCredentials(response: String) = {
    ((parse(response) \ "username").extract[String], (parse(response) \ "password").extract[String])
  }

  def getPassword(response: JValue) = {
    (response \ "password").extract[String]
  }

  def getExternalId(response: String) = {
    (parse(response) \ "id").extract[String]
  }

  def readEntity(response: HttpResponse) = {
    EntityUtils.toString(response.getEntity)
  }

  def readEntityAsJValue(response: HttpResponse) = {
    parse(readEntity(response))
  }

  def externalIdData(uuid: UUID) = {
    s"""{"type":"c8y_Serial","externalId":"$uuid"}"""
  }

  def getLoginName(response: String) = {
    ((parse(response) \ "user" \ "firstname").extract[String], (parse(response) \ "user" \ "lastname").extract[String])
  }

  def getDeviceConfig(response: String) = {
    (parse(response) \ "attributes" \ "apiConfig").extract[JArray]
  }

  def deviceCredentialsData(uuid: UUID) = {
    compact(render("id" -> uuid.toString))
  }

  def deviceInventoryData(id: String) =
    s"""
       |{
       |  "name": "$id",
       |  "type": "DEVICE",
       |  "c8y_IsDevice":{},
       |  "c8y_Hardware":{
       |    "revision": "v1.0",
       |    "serialNumber": "$id"
       |  }
       |}
    """.stripMargin

  def createKeys = {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    createKeysAsString(pk)
  }

  def createKeysAsString(pk: PrivKey) = {
    val privKey = Base64.getEncoder.encodeToString(pk.getRawPrivateKey.slice(0, 32))
    val pubKey = Base64.getEncoder.encodeToString(pk.getRawPublicKey.slice(0, 32))
    (pubKey, privKey)
  }

  @tailrec
  def registerForCumulocity(uuid: UUID): Unit = {
    logger.info("Please go to https://ubirch.cumulocity.com/apps/devicemanagement/index.html#/deviceregistration and add the device and approve it. You can use the id that is presented above.")
    logger.info("Copy this UUID " + uuid.toString + " and add it to Cumulocity and then come back here. ")
    val response = client.execute(deviceCredentialsRequest(deviceCredentialsData(uuid)))
    val code = response.getStatusLine.getStatusCode
    val deviceCredentialsEntityAsString = readEntity(response)
    logger.info(deviceCredentialsEntityAsString)
    if (code == 404) {
      Thread.sleep(5000)
      registerForCumulocity(uuid)
    } else if (code == 201) {
      val (u, p) = getDeviceCredentials(deviceCredentialsEntityAsString)
      val data = deviceInventoryData(uuid.toString)
      val response = client.execute(deviceInventoryRequest(data, u, p))
      val deviceInventoryEntityAsString = readEntity(response)
      val code = response.getStatusLine.getStatusCode
      logger.info(deviceInventoryEntityAsString)
      if (code < 300) {
        val id = getExternalId(deviceInventoryEntityAsString)
        val data = externalIdData(uuid)
        val response = client.execute(deviceExternalIdRequest(id, data, u, p))
        val deviceExternalIdEntityAsString = readEntity(response)
        logger.info(deviceExternalIdEntityAsString)
        if (code < 300) {
          WriteFileControl(
            10000,
            DeviceGenerationFileConfigs.path,
            DeviceGenerationFileConfigs.directory,
            DeviceGenerationFileConfigs.fileName,
            "",
            DeviceGenerationFileConfigs.ext
          )
            .secured { writer =>
              val (publicKey, privateKey) = createKeys
              val deviceGeneration = DeviceGeneration(
                UUID = uuid,
                deviceCredentials = parse(deviceCredentialsEntityAsString),
                deviceInventory = parse(deviceInventoryEntityAsString),
                deviceExternalId = parse(deviceExternalIdEntityAsString),
                publicKey = publicKey,
                privateKey = privateKey
              )

              if (DeviceGenerationFileConfigs.runKeyRegistration) {
                val url = "https://key." + EnvConfigs.ENV + ".ubirch.com/api/keyService/v1/pubkey"
                val (info, data, verification, resp, body) = KeyRegistration.register(url, deviceGeneration.UUID, deviceGeneration.privateKey, deviceGeneration.publicKey)
                KeyRegistration.logOutput(info, data, verification, resp, body)
              }

              val dataToStore = compact(Extraction.decompose(deviceGeneration))
              writer.append(dataToStore)

            }
          logger.info("Device registered")
        }
      } else {
        logger.error("Unexpected response code (1): " + code)
      }
    } else {
      logger.error("Unexpected response code (2): " + code)
    }
  }

  @tailrec
  def readLines(result: String): String = {
    val continue = readLine()
    continue.trim match {
      case "..." => result
      case x => readLines(result + x)
    }
  }

  def registerForConsoleAutomaticCreation: Unit = {

    logger.info("Copy your console access token here (without the \"bearer\")")
    logger.info("To finish, enter ...")
    val accessToken = readLines("")
    val response = client.execute(simpleAuthTestConsole(accessToken))
    val actualValue = readEntity(response)
    if (response.getStatusLine.getStatusCode == 200) {
      logger.info(s"Hello ${getLoginName(actualValue)._1} ${getLoginName(actualValue)._2}. Seems like your auth token is valid.")
      logger.info("How many devices would you like to create ?")
      logger.info("To finish, enter ...")
      val numberOfDevicesToAdd = readLines("").toInt
      logger.info(s"Adding $numberOfDevicesToAdd devices with random UUIDs to your console")
      logger.info("------------------------------------------")
      def addDevice = {

        val uuid = UUID.randomUUID()
        val addRequest = addDeviceInConsole(uuid, accessToken)
        val responseAdd = client.execute(addRequest)
        val bodyValue = readEntity(responseAdd)
        logger.info(s"bodyValue = $bodyValue")

        val configReq = client.execute(getDeviceConfigFromConsole(uuid, accessToken))
        val response = readEntity(configReq)
        logger.info(s"deviceConfig = $response")
        val deviceConfig = {
          val data = compact(getDeviceConfig(response).children.head)
          if (data.startsWith("\"") && data.endsWith("\"") && data.contains("\\"))
            data.stripPrefix("\"").stripSuffix("\"").replace("\\", "")
          else
            data
        }

        WriteFileControl(
          10000,
          DeviceGenerationFileConfigs.path,
          DeviceGenerationFileConfigs.directory,
          DeviceGenerationFileConfigs.fileName,
          "",
          DeviceGenerationFileConfigs.ext
        )
          .secured { writer =>
            val (publicKey, privateKey) = createKeys
            val deviceGeneration = DeviceGeneration(
              UUID = uuid,
              deviceCredentials = parse(deviceConfig),
              deviceInventory = JNothing,
              deviceExternalId = JNothing,
              publicKey = publicKey,
              privateKey = privateKey
            )
            if (DeviceGenerationFileConfigs.runKeyRegistration) {
              val url = "https://key." + EnvConfigs.ENV + ".ubirch.com/api/keyService/v1/pubkey"
              val (info, data, verification, resp, body) = KeyRegistration.register(url, deviceGeneration.UUID, deviceGeneration.privateKey, deviceGeneration.publicKey)
              KeyRegistration.logOutput(info, data, verification, resp, body)
            }
            val dataToStore = compact(Extraction.decompose(deviceGeneration))
            writer.append(dataToStore)
          }

      }

      for { i <- 1 to numberOfDevicesToAdd } {
        addDevice
        if (i % 5 == 0) logger.info(s"Added $i out of $numberOfDevicesToAdd devices")
      }
    } else {
      logger.info("Seems like the auth token is wrong. Please make sure you provide a valid access token")
    }

  }

  def registerForConsole(uuid: UUID): Unit = {
    logger.info("Please go to https://console.dev.ubirch.com/devices/list and add the device. You can use the id that is presented above.")
    logger.info("Copy this UUID " + uuid.toString + " and add it as a Thing, copy the config json and paste it here.")
    logger.info("Paste the device config. To finish enter ...")
    val config = readLines("")

    WriteFileControl(
      10000,
      DeviceGenerationFileConfigs.path,
      DeviceGenerationFileConfigs.directory,
      DeviceGenerationFileConfigs.fileName,
      "",
      DeviceGenerationFileConfigs.ext
    )
      .secured { writer =>
        val (publicKey, privateKey) = createKeys
        val deviceGeneration = DeviceGeneration(
          UUID = uuid,
          deviceCredentials = parse(config),
          deviceInventory = JNothing,
          deviceExternalId = JNothing,
          publicKey = publicKey,
          privateKey = privateKey
        )

        if (DeviceGenerationFileConfigs.runKeyRegistration) {
          val url = "https://key." + EnvConfigs.ENV + ".ubirch.com/api/keyService/v1/pubkey"
          val (info, data, verification, resp, body) = KeyRegistration.register(url, deviceGeneration.UUID, deviceGeneration.privateKey, deviceGeneration.publicKey)
          KeyRegistration.logOutput(info, data, verification, resp, body)
        }

        val dataToStore = compact(Extraction.decompose(deviceGeneration))
        writer.append(dataToStore)

      }
    logger.info("Device registered")
  }

  def go(): Unit = {
    val uuid = UUID.randomUUID()

    if (DeviceGenerationFileConfigs.consoleRegistration)
      if (DeviceGenerationFileConfigs.consoleAutomaticCreation) registerForConsoleAutomaticCreation
      else {
        logger.info("Creating device with id: " + uuid.toString)
        registerForConsole(uuid)
      }
    else {
      logger.info("Creating device with id: " + uuid.toString)
      registerForCumulocity(uuid)
    }

    def more(): Unit = {
      val continue = readLine("Add another device? Y/n ")
      continue.toLowerCase().trim match {
        case "y" => go()
        case "n" =>
        case _ => more()
      }
    }

    more()

  }

  def main(args: Array[String]): Unit = {
    logger.info("Automatic Key Registration is: " + (if (DeviceGenerationFileConfigs.runKeyRegistration) "ON" else "OFF"))
    logger.info("Console Registration is: " + (if (DeviceGenerationFileConfigs.consoleRegistration) "ON" else "OFF"))
    if (DeviceGenerationFileConfigs.consoleRegistration) {
      logger.info("Console automatic creation is: " + (if (DeviceGenerationFileConfigs.consoleAutomaticCreation) "ON" else "OFF"))
    }
    go()
  }

}
