package com.ubirch

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.{ PrivKey, PubKey }
import com.ubirch.models.{ PayloadGenerator, SimpleProtocolImpl, WriteFileControl }
import com.ubirch.util.{ ConfigBase, EnvConfigs, FileConfigs }

class Generator(clientUUID: UUID, clientKey: PrivKey, serverUUID: UUID, serverKey: PubKey) extends FileConfigs with LazyLogging {

  val protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey)
  val payloadGenerator = new PayloadGenerator(clientUUID, protocol)

  logger.info(maxNumberOfMessages / numberOfMessagesPerFile + " files will be created.")

  WriteFileControl(numberOfMessagesPerFile, path, directory, fileName, ext)
    .secured { writer =>
      Iterator
        .continually(payloadGenerator.getOneAsString)
        .take(maxNumberOfMessages)
        .foreach { case (_, upp, hash) => writer.append(upp + ";" + hash) }
    }

}

object Generator extends ConfigBase with EnvConfigs with LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Gen Started and Generating")
    new Generator(clientUUID, clientKey, serverUUID, serverKey)

  }

}
