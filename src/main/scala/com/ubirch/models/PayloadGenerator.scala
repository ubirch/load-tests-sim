package com.ubirch.models

import java.util.UUID

class PayloadGenerator(clientUUID: UUID, protocol: SimpleProtocolImpl) {

  def getOne = {
    AbstractUbirchClient.buildMessageAsString(clientUUID, protocol, (Math.random * 10 + 10).toInt)
  }
}
