package com.ubirch.util

import java.nio.charset.StandardCharsets
import java.util.{ Base64, UUID }

import com.ubirch.models._
import com.ubirch.{ DataGenerator, DeviceGenerator, KeyRegistration }

case class Data(upp: Array[Byte], hash: Array[Byte], password: String, device: UUID, auth: String)

class Continuous(deviceGenerations: Seq[DeviceGeneration], consoleRegistration: Boolean) {

  val length: Int = deviceGenerations.length

  val generators: Seq[(DeviceGeneration, PayloadGenerator)] = deviceGenerations.map { dataGeneration =>
    val clientKey = KeyRegistration.getKey(dataGeneration.privateKey)
    val payloadGenerator = DataGenerator.payloadGenerator(dataGeneration.UUID, clientKey, EnvConfigs.serverUUID, EnvConfigs.serverKey)
    (dataGeneration, payloadGenerator)
  }
  val feeder: Iterator[Map[String, String]] = Iterator.continually {

    val (deviceGeneration, payloadGenerator) = random
    lazy val auth: String = Helpers.encodedAuth(deviceGeneration.deviceCredentials)

    val (_, upp, hash) = payloadGenerator.getOneAsString

    val passwordAsBytes = DeviceGenerator
      .getPassword(deviceGeneration.deviceCredentials)
      .getBytes(StandardCharsets.UTF_8)
    val passwordAsBase64 = Base64.getEncoder.encodeToString(passwordAsBytes)

    //val dataGeneration = DataGeneration(deviceGeneration.UUID, deviceGeneration.deviceCredentials, upp, hash)

    //      WriteFileControl(
    //        DataGenerationFileConfigs.numberOfMessagesPerFile,
    //        DataGenerationFileConfigs.path,
    //        DataGenerationFileConfigs.directory,
    //        DataGenerationFileConfigs.fileName,
    //        "Verification_",
    //        DataGenerationFileConfigs.ext
    //      ).secured { w =>
    //          val dataToStore = compact(Extraction.decompose(dataGeneration))
    //          w.append(dataToStore)
    //        }

    Map(
      "UPP" -> upp,
      "HASH" -> hash,
      "password" -> passwordAsBase64,
      "hardware_id" -> deviceGeneration.UUID.toString,
      "auth" -> (if (consoleRegistration) "" else auth)
    )

  }

  val feeder2: Iterator[Data] = Iterator.continually {

    val (deviceGeneration, payloadGenerator) = random
    lazy val auth: String = Helpers.encodedAuth(deviceGeneration.deviceCredentials)
    val (_, upp, hash) = payloadGenerator.getOne
    val passwordAsBytes = DeviceGenerator
      .getPassword(deviceGeneration.deviceCredentials)
      .getBytes(StandardCharsets.UTF_8)
    val passwordAsBase64 = Base64.getEncoder.encodeToString(passwordAsBytes)

    Data(upp, hash, passwordAsBase64, deviceGeneration.UUID, (if (consoleRegistration) "" else auth))

  }

  def random: (DeviceGeneration, PayloadGenerator) = generators(scala.util.Random.nextInt(length))
}
