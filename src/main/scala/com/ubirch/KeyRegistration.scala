package com.ubirch

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.{ Base64, TimeZone, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl, SimpleProtocolImpl }
import com.ubirch.util.{ ConfigBase, DeviceGenerationFileConfigs, EnvConfigs }
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._

object KeyRegistration extends ConfigBase with DeviceGenerationFileConfigs with EnvConfigs with LazyLogging {

  implicit val formats = DefaultFormats

  val client: HttpClient = HttpClients.createMinimal()

  def pubKeyInfoData(clientUUID: UUID, df: SimpleDateFormat, sk: String) = {
    val now = System.currentTimeMillis()
    s"""
      |{
      |   "algorithm": "ECC_ED25519",
      |   "created": "${df.format(now)}",
      |   "hwDeviceId": "${clientUUID.toString}",
      |   "pubKey": "$sk",
      |   "pubKeyId": "$sk",
      |   "validNotAfter": "${df.format(now + 31557600000L)}",
      |   "validNotBefore": "${df.format(now)}"
      |}
    """.stripMargin
  }

  def registrationData(pubKeyInfoData: String, signature: String) = {
    s"""
      |{
      |   "pubKeyInfo": $pubKeyInfoData,
      |   "signature": "$signature"
      |}
    """.stripMargin
  }

  def registerKeyRequest(body: String) = {
    val regRequest = new HttpPost("https://key." + ENV + ".ubirch.com/api/keyService/v1/pubkey")
    regRequest.setHeader("Content-Type", "application/json")
    regRequest.setEntity(new StringEntity(body))
    regRequest
  }

  def getKey(privateKey: String) = {
    val clientKeyBytes = Base64.getDecoder.decode(privateKey)
    AbstractUbirchClient.createClientKey(clientKeyBytes)
  }

  def main(args: Array[String]): Unit = {

    logger.info("Key Registration Started")

    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))

    ReadFileControl(path, directory, fileName, ext).read { l =>
      l.split(";").toList match {
        case Nil => logger.info("Nothing to do")
        case List(uuidAsString, _, _, _, publicKey, privateKey) =>
          val uuid = UUID.fromString(uuidAsString)

          val clientKey = getKey(privateKey)
          val protocol = new SimpleProtocolImpl(uuid, clientKey, serverUUID, serverKey)

          val info = compact(parse(pubKeyInfoData(uuid, df, publicKey)))
          val signature = protocol.sign(uuid, info.getBytes(StandardCharsets.UTF_8))
          val data = compact(parse(registrationData(info, Base64.getEncoder.encodeToString(signature))))

          val verification = clientKey.verify(info.getBytes, signature)
          val resp = client.execute(registerKeyRequest(data))
          val body = DeviceGenerator.readEntity(resp)

          logger.info("Info: " + info)
          logger.info("Data: " + data)
          logger.info("Verification: " + verification.toString)
          logger.info("Response: " + body)
          logger.info("Status Response: " + resp.getStatusLine.getStatusCode.toString)
      }

    }

  }

}
