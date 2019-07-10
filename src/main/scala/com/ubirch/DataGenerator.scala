package com.ubirch

import java.util.{ Base64, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.{ PrivKey, PubKey }
import com.ubirch.models._
import com.ubirch.util.{ ConfigBase, DataGenerationFileConfigs, DeviceGenerationFileConfigs, EnvConfigs }

class DataGenerator(clientUUID: UUID, clientKey: PrivKey, serverUUID: UUID, serverKey: PubKey) extends DataGenerationFileConfigs with LazyLogging {

  val protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey)
  val payloadGenerator = new PayloadGenerator(clientUUID, protocol)

  logger.info(maxNumberOfMessages / numberOfMessagesPerFile + " files will be created.")

  WriteFileControl(numberOfMessagesPerFile, path, directory, fileName, ext)
    .secured { writer =>
      Iterator
        .continually(payloadGenerator.getOneAsString)
        .take(maxNumberOfMessages)
        .foreach { case (_, upp, hash) => writer.append(clientUUID.toString + ";" + upp + ";" + hash) }
    }

}

object DataGenerator extends ConfigBase with EnvConfigs with LazyLogging with DeviceGenerationFileConfigs {

  def main(args: Array[String]): Unit = {
    logger.info("Gen Started and Generating")

    ReadFileControl(path, directory, fileName, ext).read { l =>
      l.split(";").toList match {
        case Nil => logger.info("Nothing to do.")
        case List(uuidAsString, _, _, _, pk, _) =>
          val clientKeyBytes = Base64.getDecoder.decode(pk)
          val clientKey = AbstractUbirchClient.createClientKey(clientKeyBytes)
          new DataGenerator(UUID.fromString(uuidAsString), clientKey, serverUUID, serverKey)
      }

    }

  }

}
