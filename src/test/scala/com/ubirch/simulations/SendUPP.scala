package com.ubirch.simulations

import java.util.Base64

import com.ubirch.models.{ AbstractUbirchClient, ReadFileControl }
import com.ubirch.util.{ DataFileConfigs, EnvConfigs }
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.http.auth.UsernamePasswordCredentials

import scala.language.postfixOps

trait WithCredentials extends EnvConfigs {
  val credentials = new UsernamePasswordCredentials(authUser, authPass)
  val auth: String = Base64.getEncoder.encodeToString((credentials.getUserName + ":" + credentials.getPassword).getBytes)
}

trait WithProtocol extends WithCredentials {

  val httpProtocol = http
    .baseUrl("https://niomon." + ENV + ".ubirch.com")
    .authorizationHeader("Basic " + auth)

}

object SendUPP extends DataFileConfigs {

  val data = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

  ReadFileControl(path, directory, fileName, ext).read { l =>
    l.split(";").toList.headOption.foreach { x =>
      data += Map("UPP" -> x)
    }
  }

  def createBody(session: Session) = {
    val value = session("UPP").as[String]
    AbstractUbirchClient.toBytesFromHex(value)
  }

  val scn = scenario("SendUPPSimulation")
    .feed(data.toIndexedSeq.queue)
    .exec(
      http("Send UPP data")
        .post("/")
        .body(ByteArrayBody(createBody))
    )

}
