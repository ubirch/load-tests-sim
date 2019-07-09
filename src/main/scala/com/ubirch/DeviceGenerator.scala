package com.ubirch

import java.util.{ Base64, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.WriteFileControl
import com.ubirch.util.{ ConfigBase, DeviceGenerationFileConfigs }
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json4s.DefaultFormats
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.io.StdIn.readLine

import scala.annotation.tailrec

object DeviceGenerator extends ConfigBase with DeviceGenerationFileConfigs with LazyLogging {

  implicit val formats = DefaultFormats

  def encode(data: String) = Base64.getEncoder.encodeToString(data.getBytes)

  val auth = encode("devicebootstrap:" + "Fhdt1bb1f")

  val client: HttpClient = HttpClients.createMinimal()

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

  def getExternalId(response: String) = {
    (parse(response) \ "id").extract[String]
  }

  def readEntity(response: HttpResponse) = {
    EntityUtils.toString(response.getEntity)
  }

  def externalIdData(uuid: UUID) = {
    s"""{"type":"c8y_Serial","externalId":"$uuid"}"""
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

  @tailrec
  def register(uuid: UUID): Unit = {
    val response = client.execute(deviceCredentialsRequest(deviceCredentialsData(uuid)))
    val code = response.getStatusLine.getStatusCode
    val entityAsString = readEntity(response)
    logger.info(entityAsString)
    if (code == 404) {
      Thread.sleep(5000)
      register(uuid)
    } else if (code == 201) {
      val (u, p) = getDeviceCredentials(entityAsString)
      val data = deviceInventoryData(uuid.toString)
      val response = client.execute(deviceInventoryRequest(data, u, p))
      val entityAsString2 = readEntity(response)
      val code = response.getStatusLine.getStatusCode
      logger.info(entityAsString2)
      if (code < 300) {
        val id = getExternalId(entityAsString2)
        val data = externalIdData(uuid)
        val response = client.execute(deviceExternalIdRequest(id, data, u, p))
        val entityAsString3 = readEntity(response)
        logger.info(entityAsString3)
        if (code < 300) {
          WriteFileControl(10000, path, directory, fileName, ext)
            .secured { writer =>
              writer.append(uuid.toString + ";" + entityAsString + ";" + entityAsString2 + ";" + entityAsString3)
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

  def go(): Unit = {
    val uuid = UUID.randomUUID()
    logger.info("Creating device with id: " + uuid.toString)
    logger.info("Please go to https://ubirch.cumulocity.com/apps/devicemanagement/index.html#/deviceregistration and add the device and approve it." +
      "You can use the id that is presented above.")

    logger.info(uuid.toString)
    register(uuid)

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
    go()
  }

}
