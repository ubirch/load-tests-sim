package com.ubirch

import com.google.protobuf.ByteString
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.models.{DeviceGeneration, FlowInPayload, FlowOutPayload}
import com.ubirch.util.{ConfigBase, Continuous, MqttConf}
import monix.eval.Task
import org.joda.time.{DateTime, Duration}
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import scala.language.postfixOps

object MqttInjector extends LazyLogging with ConfigBase {

  def inTopic(deviceId: UUID): String = Paths.get(MqttConf.IN_QUEUE_PREFIX, deviceId.toString).toString
  def outTopic(deviceId: UUID): String = Paths.get(MqttConf.OUT_QUEUE_PREFIX, deviceId.toString).toString
  def outAllTopic: String = Paths.get(MqttConf.OUT_QUEUE_PREFIX, "+").toString

  def go(max: Int, whenLog: Int, clid: String, devices: List[DeviceGeneration]) = {

    val consoleRegistration: Boolean = conf.getBoolean("deviceGenerator.consoleRegistration")
    val continuous = new Continuous(devices, consoleRegistration)

    val mqtt: MqttClients = new DefaultMqttClients
    mqtt.buildClient(clid)

    val range = 1 to max

    val startTime = new DateTime()
    logger.info("Starting @ " + startTime.toString + " max=" + max)

    val received = new AtomicInteger(0)
    mqtt.subscribe(outAllTopic, 1)((_, m) => {
      val crr = received.incrementAndGet()
      if ((crr % whenLog) == 0) {
        val endTime = new DateTime()
        val diffInMillis = (endTime.getMillis - startTime.getMillis) / whenLog
        val pl = FlowOutPayload.parseFrom(m.getPayload)
        logger.info(s"($clid) avg_processing ->" +
          " time_per_message=" + diffInMillis +
          " ms processed_messages=" + crr.toString +
          " status=" + pl.status)
      }

      if(crr == max) {
        val endTime = new DateTime()
        logger.info(s"($clid) DONE - " + new Duration(endTime, startTime).abs().toString)
      }
      Task.delay(m)
    })

    range.map { _ =>
      val data = continuous.feeder2.take(1).toList.headOption.get
      val payload = FlowInPayload(data.device.toString, data.password, ByteString.copyFrom(data.upp)).toByteArray
      val mqttPayload = mqtt.toMqttMessage(qos = 2, retained = false, payload = payload)
      Thread.sleep(1)
      mqtt.publish(inTopic(data.device), data.device, mqttPayload)
    }

  }


  def splitDevices(maxClients: Int, devices: List[DeviceGeneration]): Vector[List[DeviceGeneration]] = {
    if(maxClients < devices.size) {
      val buckets = devices.size / maxClients
      val splits = (0 to maxClients).map { i =>
        devices.slice(buckets * i, buckets * (i + 1))
      }.toVector
      require(splits.size == maxClients)
      splits
    } else {
      (0 until maxClients).map(_ => devices).toVector
    }
  }

  def main(args: Array[String]): Unit = {

    def cli = MqttConf.CLI
    def max = MqttConf.MAX
    def whenLog = MqttConf.WHEN_LOG
    def maxClients = MqttConf.MAX_CLIENTS

    val onlyTheseDevices: List[String] = conf.getString("simulationDevices").split(",").toList.filter(_.nonEmpty)
    val devices: List[DeviceGeneration] = DeviceGenerator.loadDevices(onlyTheseDevices)
    val deviceBuckets: Vector[List[DeviceGeneration]] = splitDevices(maxClients, devices)
    deviceBuckets.zipWithIndex.par.foreach{ case (devices, i) => go(max, whenLog, cli + i, devices) }

    new CountDownLatch(1).await()

  }

}

