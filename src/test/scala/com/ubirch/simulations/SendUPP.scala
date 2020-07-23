package com.ubirch.simulations

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.AbstractUbirchClient
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.language.postfixOps

trait SendUPP extends Common with LazyLogging {

  private def createBody(session: Session): Array[Byte] = {
    val value = session("UPP").as[String]
    AbstractUbirchClient.toBytesFromHex(value)
  }

  private def authHeader(session: Session): String = {
    val auth = session("auth").as[String]
    "Basic " + auth
  }

  private val send: HttpRequestBuilder = {
    http("Send UPP data")
      .post("/")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationOctetStream)
      .header("X-Ubirch-Hardware-Id", session => session("hardware_id").as[String])
      .header("X-Ubirch-Auth-Type", "ubirch")
      .header("X-Ubirch-Credential", session => session("password").as[String])
      .header("Authorization", session => authHeader(session))
      .body(ByteArrayBody(createBody))
  }

  def sendScenarioWithFileData(suffixes: List[String]): ScenarioBuilder = {
    getScenarioWithFileData("Device Message (UPPs)", suffixes, send)
  }

  def sendScenarioWithContinuousData(continuous: Continuous): ScenarioBuilder = {
    getScenarioWithContinuousData("Device Message (Continuous UPPs)", continuous, send)
  }

}
