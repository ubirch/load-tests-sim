package com.ubirch

import java.util.{ Base64, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.util.{ ConfigBase, DeviceGenerationFileConfigs }
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.annotation.tailrec

object DeviceGenerator extends ConfigBase with DeviceGenerationFileConfigs with LazyLogging {

  val auth = Base64.getEncoder.encodeToString(("devicebootstrap:" + "Fhdt1bb1f").getBytes)

  val client: HttpClient = HttpClients.createMinimal()

  @tailrec
  def check(request: HttpPost): Unit = {
    val response = client.execute(request)
    val code = response.getStatusLine.getStatusCode
    readEntity(response)
    if (code == 404) {
      Thread.sleep(5000)
      check(request)
    } else if (code == 201) {}
    else {
      logger.error("Unexpected response code: " + code)
    }
  }

  def readEntity(response: HttpResponse) = {
    val res = EntityUtils.toString(response.getEntity)
    logger.info(res)
  }

  def request(data: String) = {
    val req = new HttpPost("https://ubirch.cumulocity.com/devicecontrol/deviceCredentials")
    req.addHeader("Content-Type", "application/vnd.com.nsn.cumulocity.deviceCredentials+json")
    req.addHeader("Accept", "application/vnd.com.nsn.cumulocity.deviceCredentials+json")
    req.addHeader("Authorization", "Basic " + auth)
    req.setEntity(new StringEntity(data))
    req
  }

  def main(args: Array[String]): Unit = {

    val uuids = (0 to numberOfDevices).map { _ => UUID.randomUUID() }

    val data = uuids.map(x => compact(render("id" -> x.toString)))

    logger.info(s"Starting process for ${uuids.size} devices")
    data.foreach { x =>
      logger.info(x)
      Thread.sleep(3000)
      check(request(x))
    }

  }

}
