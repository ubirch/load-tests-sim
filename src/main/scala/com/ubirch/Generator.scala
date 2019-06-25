package com.ubirch

import java.util.{Base64, UUID}

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.{PrivKey, PubKey}
import com.ubirch.models.{AbstractUbirchClient, FileControl, PayloadGenerator, SimpleProtocolImpl}
import com.ubirch.util.ConfigBase

class Generator(clientUUID: UUID, clientKey: PrivKey, serverUUID: UUID, serverKey: PubKey) extends ConfigBase with LazyLogging {

  val numberOfMessagesPerFile: Int = conf.getInt("generator.numberOfMessagesPerFile")
  val maxNumberOfMessages: Int = conf.getInt("generator.maxNumberOfMessages")
  val path: String = conf.getString("generator.path")
  val fileName: String = conf.getString("generator.fileName")
  val ext: String = conf.getString("generator.ext")

  val protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey)
  val payloadGenerator = new PayloadGenerator(clientUUID, protocol)

  logger.info(maxNumberOfMessages / numberOfMessagesPerFile + " files will be created.")

  FileControl(numberOfMessagesPerFile, path, fileName, ext)
    .secured { writer =>
      Iterator
        .continually(payloadGenerator.getOne)
        .take(maxNumberOfMessages)
        .foreach { case (_, upp, hash) => writer.append(upp + ";" + hash) }
    }
}

object Generator extends ConfigBase with LazyLogging {

  def main(args: Array[String]): Unit = {

    logger.info("Gen Started>")

    val envVars = System.getenv()

    logger.info("Reading Env Values")

    val serverUUID = UUID.fromString(envVars.getOrDefault("SERVER_UUID", "9d3c78ff-22f3-4441-a5d1-85c636d486ff"))
    val clientUUID = UUID.fromString(envVars.get("CLIENT_UUID"))
    val serverKeyBytes = Base64.getDecoder.decode(envVars.getOrDefault("SERVER_PUBKEY", "okA7krya3TZbPNEv8SDQIGR/hOppg/mLxMh+D0vozWY="))
    val clientKeyBytes = Base64.getDecoder.decode(envVars.get("CLIENT_KEY"))

    // ===== DECODE AND SET UP KEYS =========================================================
    // Keys should be created and stored in a KeyStore for optimal security
    val serverKey = AbstractUbirchClient.createServerKey(serverKeyBytes) // server public key for verification of responses
    val clientKey = AbstractUbirchClient.createClientKey(clientKeyBytes) // client signing key for signing messages

    logger.info("Generating")

    new Generator(clientUUID, clientKey, serverUUID, serverKey)

  }

}
