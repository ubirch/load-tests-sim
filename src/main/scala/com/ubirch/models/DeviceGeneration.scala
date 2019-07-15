package com.ubirch.models

import java.util.UUID

import org.json4s.JValue

case class DeviceGeneration(UUID: UUID, deviceCredentials: JValue, deviceInventory: JValue, deviceExternalId: JValue, publicKey: String, privateKey: String)
