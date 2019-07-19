package com.ubirch.simulations

import com.typesafe.scalalogging.LazyLogging
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.language.postfixOps

trait VerifyUPP extends Common with LazyLogging {

  val verify = {
    http("Verify UPP data")
      .post("")

      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationFormUrlEncoded)
      .body(StringBody(session => session("HASH").as[String]))
      .check(bodyString.find.notNull)

  }

  def verifyScenario(suffixes: List[String]) = {
    getScenario("Device Verification (UPP)", suffixes, verify)
  }

}
