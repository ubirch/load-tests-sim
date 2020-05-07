package com.ubirch.util

import java.util.{ Base64, UUID }

import com.ubirch.crypto.PubKey
import com.ubirch.models.AbstractUbirchClient

trait DataGenerationFileConfigs extends ConfigBase {
  val numberOfMessagesPerFile: Int = conf.getInt("generator.numberOfMessagesPerFile")
  val maxNumberOfMessages: Int = conf.getInt("generator.maxNumberOfMessages")
  val path: String = conf.getString("generator.path")
  val directory: String = conf.getString("generator.directory")
  val fileName: String = conf.getString("generator.fileName")
  val ext: String = conf.getString("generator.ext")
}

trait DeviceGenerationFileConfigs extends ConfigBase {
  val deviceBootstrap: String = conf.getString("devicebootstrap")
  val path: String = conf.getString("deviceGenerator.path")
  val directory: String = conf.getString("deviceGenerator.directory")
  val fileName: String = conf.getString("deviceGenerator.fileName")
  val ext: String = conf.getString("deviceGenerator.ext")
  val runKeyRegistration: Boolean = conf.getBoolean("deviceGenerator.runKeyRegistration")
  val consoleRegistration: Boolean = conf.getBoolean("deviceGenerator.consoleRegistration")
  val consoleAutomaticCreation: Boolean = conf.getBoolean("deviceGenerator.consoleAutomaticCreation")
}

trait EnvConfigs extends ConfigBase {

  val ENV: String = conf.getString("environment")
  val serverUUID: UUID = UUID.fromString(conf.getString("server_uuid"))
  val serverKeyBytes: Array[Byte] = Base64.getDecoder.decode(conf.getString("server_pubkey"))
  val serverKey: PubKey = AbstractUbirchClient.createServerKey(serverKeyBytes)

}
