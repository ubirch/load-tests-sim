package com.ubirch

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.KeyRegistration.getKey
import com.ubirch.crypto.{ PrivKey, PubKey }
import com.ubirch.models._
import com.ubirch.util.{ ConfigBase, DataGenerationFileConfigs, DeviceGenerationFileConfigs, EnvConfigs }

import scala.util.{ Failure, Success, Try }

private class Total {
  @volatile private var added = 0
  def inc: Unit = added = added + 1
  def total: Int = added
}

class DataGenerator(total: Total, clientUUID: UUID, deviceCredentials: String, clientKey: PrivKey, serverUUID: UUID, serverKey: PubKey) extends DataGenerationFileConfigs with LazyLogging {

  val protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey)
  val payloadGenerator = new PayloadGenerator(clientUUID, protocol)

  val filesToCreate = Try(maxNumberOfMessages / numberOfMessagesPerFile).map(x => if (x == 0) 1 else x)

  filesToCreate match {
    case Success(ftc) =>

      logger.info("Device " + clientUUID + " | " + maxNumberOfMessages + " max messages | " + ftc + " file(s) will be created/modified")

      WriteFileControl(numberOfMessagesPerFile, path, directory, fileName, ext)
        .secured { writer =>
          Iterator
            .continually(payloadGenerator.getOneAsString)
            .take(maxNumberOfMessages)
            .foreach { case (_, upp, hash) =>
              writer.append(clientUUID.toString + ";" + deviceCredentials + ";" + upp + ";" + hash)
              total.inc
            }
        }

    case Failure(_) => logger.error("Wrong maxNumberOfMessages and/or numberOfMessagesPerFile")
  }

}

object DataGenerator extends ConfigBase with EnvConfigs with LazyLogging with DeviceGenerationFileConfigs {

  def main(args: Array[String]): Unit = {
    logger.info("Gen Started and Generating")

    val total = new Total

    ReadFileControl(path, directory, fileName, ext).read { l =>
      l.split(";").toList match {
        case Nil => logger.info("Nothing to do.")
        case List(uuidAsString, deviceCredentials, _, _, _, privateKey) =>
          val clientKey = getKey(privateKey)
          new DataGenerator(total, UUID.fromString(uuidAsString), deviceCredentials, clientKey, serverUUID, serverKey)
      }

    }

    logger.info("Gen Done: " + total.total + " messages added.")

  }

}
