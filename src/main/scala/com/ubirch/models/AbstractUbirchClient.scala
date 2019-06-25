package com.ubirch.models

import java.io.ByteArrayOutputStream
import java.security.{ InvalidKeyException, MessageDigest, NoSuchAlgorithmException }
import java.text.SimpleDateFormat
import java.util.{ Base64, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey, PubKey }
import com.ubirch.crypto.utils.Curve
import com.ubirch.protocol.{ Protocol, ProtocolMessage }
import org.apache.commons.codec.binary.Hex
import org.apache.http.HttpResponse
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity

abstract class AbstractUbirchClient(protocol: SimpleProtocolImpl, credentials: UsernamePasswordCredentials, client: HttpClient) extends LazyLogging {

  def buildMessage(clientUUID: UUID, temp: Int): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    AbstractUbirchClient.buildMessage(clientUUID, protocol, temp)
  }

  def buildHttpRequest(ENV: String, dataToSend: Array[Byte]): HttpPost = {
    val postRequest = new HttpPost("https://niomon." + ENV + ".ubirch.com")
    // we need to force authentication here, httpclient4 will not send it by it's own
    val auth = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)
    postRequest.setHeader("Authorization", "Basic " + auth)
    postRequest.setEntity(new ByteArrayEntity(dataToSend))
    postRequest
  }

  def sendRequest(request: HttpPost): HttpResponse = client.execute(request)

  def readResponse(response: HttpResponse): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    response.getEntity.writeTo(baos)
    val responseUPP = baos.toByteArray
    responseUPP
  }

  def send(ENV: String, clientUUID: UUID, temp: Int): Unit = {

    logger.debug("Building message")
    val (_, upp, hash) = buildMessage(clientUUID, temp)
    val request = buildHttpRequest(ENV, upp)
    logger.debug("Sending UBIRCH Protocol Packet (UPP)")
    logger.debug("Current Hash: " + AbstractUbirchClient.toBase64(hash))
    val sentRequest = sendRequest(request)
    val response = readResponse(sentRequest)
    logger.debug("REQUEST: UPP(" + AbstractUbirchClient.toHex(upp) + ")")
    logger.debug("RESPONSE: UPP(" + AbstractUbirchClient.toHex(response) + ")")
    logger.debug("Decoded and verified server response:" + protocol.decodeVerify(response))

  }

}

object AbstractUbirchClient extends LazyLogging {

  def toBase64(data: Array[Byte]): String = Base64.getEncoder.encodeToString(data)
  def toHex(data: Array[Byte]): String = Hex.encodeHexString(data)
  def createServerKey(serverKeyBytes: Array[Byte]): PubKey = {
    try
      GeneratorKeyFactory.getPubKey(serverKeyBytes, Curve.Ed25519)
    catch {
      case e @ (_: NoSuchAlgorithmException | _: InvalidKeyException) =>
        logger.error("Missing or broken SERVER_PUBKEY (base64)")
        throw e
    }
  }

  def createClientKey(clientKeyBytes: Array[Byte]): PrivKey = {
    try
      GeneratorKeyFactory.getPrivKey(clientKeyBytes, Curve.Ed25519)
    catch {
      case e @ (_: NoSuchAlgorithmException | _: InvalidKeyException) =>
        logger.error("Missing or broken CLIENT_KEY (base64)")
        throw e
    }
  }

  def buildMessage(clientUUID: UUID, protocol: SimpleProtocolImpl, temp: Int): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val ts = System.currentTimeMillis
    val c8yMessage = temp + "," + df.format(ts)
    val hash = MessageDigest.getInstance("SHA-512").digest((c8yMessage + "," + clientUUID.toString).getBytes)
    val pm = new ProtocolMessage(ProtocolMessage.SIGNED, clientUUID, 0x00, hash)
    val upp = protocol.encodeSign(pm, Protocol.Format.MSGPACK)
    (pm, upp, hash)
  }

  def buildMessageAsString(clientUUID: UUID, protocol: SimpleProtocolImpl, temp: Int): (ProtocolMessage, String, String) = {
    val (pm, upp, hash) = buildMessage(clientUUID, protocol, temp)
    (pm, AbstractUbirchClient.toHex(upp), AbstractUbirchClient.toBase64(hash))
  }

}
