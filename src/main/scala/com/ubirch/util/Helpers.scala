package com.ubirch.util

import java.util.Base64

import com.ubirch.DeviceGenerator
import org.apache.http.auth.UsernamePasswordCredentials
import org.json4s.JValue
import org.json4s.jackson.JsonMethods._

object Helpers {

  def encodedAuth(deviceCredentials: String): String = {
    val (username, password) = DeviceGenerator.getDeviceCredentials(deviceCredentials)
    val credentials = new UsernamePasswordCredentials(username, password)
    val auth: String = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)
    auth
  }

  def encodedAuth(deviceCredentials: JValue): String = {
    encodedAuth(compact(deviceCredentials))
  }

}
