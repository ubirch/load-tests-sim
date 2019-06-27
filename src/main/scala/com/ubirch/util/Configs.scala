package com.ubirch.util

import java.util.{ Base64, UUID }

import com.ubirch.models.AbstractUbirchClient

trait DataFileConfigs extends ConfigBase {
  val numberOfMessagesPerFile: Int = conf.getInt("generator.numberOfMessagesPerFile")
  val maxNumberOfMessages: Int = conf.getInt("generator.maxNumberOfMessages")
  val path: String = conf.getString("generator.path")
  val directory: String = conf.getString("generator.directory")
  val fileName: String = conf.getString("generator.fileName")
  val ext: String = conf.getString("generator.ext")
}

trait EnvConfigs {
  val envVars = System.getenv()

  val ENV: String = envVars.getOrDefault("UBIRCH_ENV", "dev")
  val serverUUID = UUID.fromString(envVars.getOrDefault("SERVER_UUID", "9d3c78ff-22f3-4441-a5d1-85c636d486ff"))
  val clientUUID = UUID.fromString(envVars.get("CLIENT_UUID"))
  val serverKeyBytes = Base64.getDecoder.decode(envVars.getOrDefault("SERVER_PUBKEY", "okA7krya3TZbPNEv8SDQIGR/hOppg/mLxMh+D0vozWY="))
  val clientKeyBytes = Base64.getDecoder.decode(envVars.get("CLIENT_KEY"))

  // ===== DECODE AND SET UP KEYS =========================================================
  // Keys should be created and stored in a KeyStore for optimal security
  val serverKey = AbstractUbirchClient.createServerKey(serverKeyBytes) // server public key for verification of responses
  val clientKey = AbstractUbirchClient.createClientKey(clientKeyBytes) // client signing key for signing messages

  val authUser: String = "device_" + clientUUID.toString
  val authPass: String = envVars.get("AUTH_PASS")

}
