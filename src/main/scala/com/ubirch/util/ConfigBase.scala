package com.ubirch.util

import com.typesafe.config.{ Config, ConfigFactory }

trait ConfigBase {
  def conf: Config = ConfigFactory.load()
}
