package com.ubirch.util

import com.typesafe.config.{ Config, ConfigFactory }

trait ConfigBase {
  lazy val conf: Config = ConfigFactory.load()
}
