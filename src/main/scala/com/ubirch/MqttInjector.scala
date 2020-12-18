package com.ubirch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.util.{ConfigBase, Continuous, MqttConf}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Date, UUID}

import com.google.protobuf.ByteString
import com.ubirch.models.{DeviceGeneration, FlowInPayload, FlowOutPayload}
import monix.eval.Task
import org.apache.commons.codec.binary.Hex
import org.joda.time.DateTime

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object MqttInjector extends LazyLogging with ConfigBase {

  def inTopic(deviceId: UUID): String = Paths.get(MqttConf.IN_QUEUE_PREFIX, deviceId.toString).toString
  def outTopic(deviceId: UUID): String = Paths.get(MqttConf.OUT_QUEUE_PREFIX, deviceId.toString).toString
  def outAllTopic: String = Paths.get(MqttConf.OUT_QUEUE_PREFIX, "+").toString

  val ec: Executions = new DefaultExecutions(1)

  def go(clid: String, devices: List[DeviceGeneration]) = {

    val consoleRegistration: Boolean = conf.getBoolean("deviceGenerator.consoleRegistration")
    val continuous = new Continuous(devices, consoleRegistration)

    val mqtt: MqttClients = new DefaultMqttClients
    mqtt.buildClient(clid)

    ec.scheduler.scheduleAtFixedRate(0 seconds, 10 seconds) {
      val next = mqtt.getSent
      logger.info(clid + "=" + next.toString)
    }

    val range = 1 to 5000

    val now = new DateTime()
    logger.info(now.toString)
    logger.info(mqtt.getSent.toString) //-- 0

    val ct = new AtomicInteger(0)
    mqtt.subscribe(outAllTopic, 1)((t, m) =>  {
      val crr = ct.incrementAndGet()
      val rr = crr % 500
      if(rr == 0) {
        val now2 = new DateTime()
        val diffInMillis = (now2.getMillis - now.getMillis)/500
        val pl = FlowOutPayload.parseFrom(m.getPayload)
        logger.info(s"avg_processing time_per_message=" + diffInMillis + " ms processed_messages=" + crr.toString + " status=" + pl.status)

      }
      Task.delay(m)
    })

    range.map { _ =>
      val uuid = UUID.randomUUID()
      val data = continuous.feeder2.take(1).toList.headOption.get
      val payload = FlowInPayload(data.device.toString, data.password, ByteString.copyFrom(data.upp)).toByteArray
      val mqttPayload = mqtt.toMqttMessage(qos = 1, retained = true, payload = payload)
      mqtt.publish(inTopic(uuid), uuid, mqttPayload)
    }

  }

  def main(args: Array[String]): Unit = {

    val onlyTheseDevices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)
    val devices: List[DeviceGeneration] = DeviceGenerator.loadDevices(onlyTheseDevices)
    (0 to 0).foreach(i => go("client_load_test_" + i, devices))

  }

}

