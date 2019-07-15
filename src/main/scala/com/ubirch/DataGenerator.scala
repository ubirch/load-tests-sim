package com.ubirch

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.KeyRegistration.getKey
import com.ubirch.crypto.{ PrivKey, PubKey }
import com.ubirch.models._
import com.ubirch.util._
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods._

import scala.util.{ Failure, Success, Try }

private class Total {
  @volatile private var added = 0
  def inc: Unit = added = added + 1
  def total: Int = added
}

class DataGenerator(total: Total, deviceGeneration: DeviceGeneration, clientKey: PrivKey, serverUUID: UUID, serverKey: PubKey)
  extends DataGenerationFileConfigs with WithJsonFormats with LazyLogging {

  val protocol = new SimpleProtocolImpl(deviceGeneration.UUID, clientKey, serverUUID, serverKey)
  val payloadGenerator = new PayloadGenerator(deviceGeneration.UUID, protocol)

  val filesToCreate = Try(maxNumberOfMessages / numberOfMessagesPerFile).map(x => if (x == 0) 1 else x)

  filesToCreate match {
    case Success(ftc) =>

      logger.info("Device " + deviceGeneration.UUID + " | " + maxNumberOfMessages + " max messages | " + ftc + " file(s) will be created/modified")

      WriteFileControl(numberOfMessagesPerFile, path, directory, fileName, ext)
        .secured { writer =>
          Iterator
            .continually(payloadGenerator.getOneAsString)
            .take(maxNumberOfMessages)
            .foreach { case (_, upp, hash) =>
              val data = DataGeneration(deviceGeneration.UUID, deviceGeneration.deviceCredentials, upp, hash)
              val dataToStore = compact(Extraction.decompose(data))
              writer.append(dataToStore)
              total.inc
            }
        }

    case Failure(_) => logger.error("Wrong maxNumberOfMessages and/or numberOfMessagesPerFile")
  }

}

object DataGenerator extends ConfigBase with EnvConfigs with DeviceGenerationFileConfigs with WithJsonFormats with LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Gen Started and Generating")

    val total = new Total

    ReadFileControl(path, directory, fileName, ext).read { l =>

      val dataGeneration = parse(l).extractOpt[DeviceGeneration].getOrElse(throw new Exception("Something wrong happened when reading data"))
      val clientKey = getKey(dataGeneration.privateKey)
      new DataGenerator(total, dataGeneration, clientKey, serverUUID, serverKey)

    }

    logger.info("Gen Done: " + total.total + " messages added.")

  }

}
