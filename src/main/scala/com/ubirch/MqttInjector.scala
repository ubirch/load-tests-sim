package com.ubirch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.util.MqttConf

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object MqttInjector extends LazyLogging {

  def inTopic(deviceId: UUID): String = Paths.get(MqttConf.IN_QUEUE_PREFIX, deviceId.toString).toString
  def outTopic(deviceId: UUID): String = Paths.get(MqttConf.OUT_QUEUE_PREFIX, deviceId.toString).toString

  val ec: Executions = new DefaultExecutions(1)

  def go(clid: String) = {

    val mqtt: MqttClients = new DefaultMqttClients
    mqtt.buildClient(clid)

    ec.scheduler.scheduleAtFixedRate(0 seconds, 10 seconds){
      val next = mqtt.getSent
      logger.info(clid + "=" + next.toString)
    }

    val range = 1 to 10000
    range.map { _ =>

      val uuid = UUID.randomUUID()
      val payload = UUID.randomUUID().toString.getBytes(StandardCharsets.UTF_8)
      val mqttPayload = mqtt.toMqttMessage(qos = 1, retained = true, payload = payload)
      mqtt.publish(inTopic(uuid), uuid, mqttPayload)

    }
  }

  def main(args: Array[String]): Unit = {
    (0 to 0).foreach(i => go("client_load_test_" + i))
  }

}

