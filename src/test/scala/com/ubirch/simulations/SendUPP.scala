package com.ubirch.simulations

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.AbstractUbirchClient
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.language.postfixOps

trait SendUPP extends Common with LazyLogging {

  def createBody(session: Session) = {
    val value = session("UPP").as[String]
    AbstractUbirchClient.toBytesFromHex(value)
  }

  def authHeader(session: Session) = {
    val auth = session("auth").as[String]
    "Basic " + auth
  }

  val send = {
    http("Send UPP data")
      .post("/")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationOctetStream)
      .header("X-Ubirch-Hardware-Id", session => session("hardware_id").as[String])
      .header("X-Ubirch-Auth-Type", "ubirch")
      .header("X-Ubirch-Credential", session => session("password").as[String])
      .header("Authorization", session => authHeader(session))
      .body(ByteArrayBody(createBody))
  }

  def sendScenario(suffixes: List[String]) = {
    getScenario("Device Message (UPP)", suffixes, send)
  }

}
