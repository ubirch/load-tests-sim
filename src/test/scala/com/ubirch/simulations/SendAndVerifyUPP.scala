package com.ubirch.simulations

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.AbstractUbirchClient
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

trait SendAndVerifyUPP extends Common with LazyLogging with Protocols {

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
      .header("Authorization", session => authHeader(session))
      .body(ByteArrayBody(createBody))
  }

  val verify = {
    http("Verify UPP data")
      .post(verifyUrl)
      .body(StringBody { session =>
        val hash = session("HASH").as[String]
        println("HASH::: " + hash)
        hash
      }).transformResponse {
        (s, r) =>
          println("STATUS::: " + r.status)
          r
      }
      .check(bodyString.find.notNull)

  }

  def sendAndVerifyScenario(suffixes: List[String]) = {
    scenario("Device Message and Verify(UPP)")
      .feed(prepareData(suffixes))
      .exec(send)
      .pause(10 seconds)
      .exec(verify)
  }

}
