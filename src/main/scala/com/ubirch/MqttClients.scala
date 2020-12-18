package com.ubirch

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.util.MqttConf
import monix.eval.Task
import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import java.util.UUID
import java.util.concurrent.{ CountDownLatch, TimeUnit }

trait MqttClients {

  def buildClient(clientId: String): IMqttAsyncClient

  def toMqttMessage(qos: Int, retained: Boolean, payload: Array[Byte]): MqttMessage

  def publish(topic: String, deviceId: UUID, message: MqttMessage): IMqttDeliveryToken

  def subscribe(topic: String, qos: Int)(process: (String, MqttMessage) => Task[MqttMessage]): Unit

  def getSent: Long

  def listener(success: IMqttToken => Unit, failure: (IMqttToken, Throwable) => Unit): IMqttActionListener = new IMqttActionListener {
    override def onSuccess(asyncActionToken: IMqttToken): Unit = {
      success(asyncActionToken)
    }

    override def onFailure(asyncActionToken: IMqttToken, exception: Throwable): Unit = {
      failure(asyncActionToken, exception)
    }
  }

}

class DefaultMqttClients extends MqttClients with LazyLogging {

  @volatile private var sent: Long = 0

  override def getSent: Long = sent

  private val broker = MqttConf.BROKER_URL
  private val userName = MqttConf.USER_NAME
  private val password = MqttConf.PASSWORD
  private var client: IMqttAsyncClient = _

  def buildClient(clientId: String): IMqttAsyncClient = {
    val p = new CountDownLatch(1)
    val c = try {
      val persistence = new MemoryPersistence()
      client = new MqttAsyncClient(broker, clientId, persistence)
      val connOpts = new MqttConnectOptions()
      connOpts.setUserName(userName)
      connOpts.setPassword(password.toCharArray)
      connOpts.setMaxInflight(100000)
      connOpts.setCleanSession(true)
      client.connect(connOpts, null, listener(_ => {
        logger.info(s"mqtt_connected=OK @ $broker")
        p.countDown()
      }, (_, e) => {
        logger.error(s"error_connecting to $broker", e)
        p.countDown()
      }))
      client
    } catch {
      case me: MqttException =>
        p.countDown()
        logger.error("retrieving MQTT client failed: ", me)
        throw me
    }

    p.await(15, TimeUnit.SECONDS)
    c

  }

  override def toMqttMessage(qos: Int, retained: Boolean, payload: Array[Byte]): MqttMessage = {
    val message = new MqttMessage(payload)
    message.setQos(qos)
    message.setRetained(retained)
    message
  }

  override def publish(topic: String, deviceId: UUID, message: MqttMessage): IMqttDeliveryToken = {
    if (!client.isConnected) throw new Exception("MQTT not connected")
    client.publish(
      topic,
      message,
      null,
      listener(
        _ => { sent = sent + 1 },
        (_, e) => logger.error(s"error_publishing_to=$topic", e)
      )
    )
  }

  override def subscribe(topic: String, qos: Int)(process: (String, MqttMessage) => Task[MqttMessage]): Unit = {
    if (!client.isConnected) throw new Exception("MQTT not connected")
    client.subscribe(
      topic,
      qos,
      null,
      listener(_ => logger.info(s"subscribed_to=$topic"), (_, e) => logger.error(s"error_subscribing_to=$topic", e)),
      (topic: String, message: MqttMessage) => process(topic, message)
    )
  }

  sys.addShutdownHook(
    if (client != null && client.isConnected) {
      logger.info("Shutting mqtt connection...")
      client.disconnect()
      client.close()
    }
  )

}

