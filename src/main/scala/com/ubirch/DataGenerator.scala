package com.ubirch

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.KeyRegistration.getKey
import com.ubirch.crypto.{ PrivKey, PubKey }
import com.ubirch.models._
import com.ubirch.util.{ ConfigBase, DataGenerationFileConfigs, DeviceGenerationFileConfigs, EnvConfigs }

class DataGenerator(clientUUID: UUID, deviceCredentials: String, clientKey: PrivKey, serverUUID: UUID, serverKey: PubKey) extends DataGenerationFileConfigs with LazyLogging {

  val protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey)
  val payloadGenerator = new PayloadGenerator(clientUUID, protocol)

  logger.info(maxNumberOfMessages / numberOfMessagesPerFile + " files will be created.")

  WriteFileControl(numberOfMessagesPerFile, path, directory, fileName, ext)
    .secured { writer =>
      Iterator
        .continually(payloadGenerator.getOneAsString)
        .take(maxNumberOfMessages)
        .foreach { case (_, upp, hash) => writer.append(clientUUID.toString + ";" + deviceCredentials + ";" + upp + ";" + hash) }
    }

}

object DataGenerator extends ConfigBase with EnvConfigs with LazyLogging with DeviceGenerationFileConfigs {

  def main(args: Array[String]): Unit = {
    logger.info("Gen Started and Generating")

    ReadFileControl(path, directory, fileName, ext).read { l =>
      l.split(";").toList match {
        case Nil => logger.info("Nothing to do.")
        case List(uuidAsString, deviceCredentials, _, _, _, privateKey) =>
          val clientKey = getKey(privateKey)
          new DataGenerator(UUID.fromString(uuidAsString), deviceCredentials, clientKey, serverUUID, serverKey)
      }

    }

  }

}
