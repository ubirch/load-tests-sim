package com.ubirch.models

import java.io.BufferedWriter
import java.io.FileWriter

import com.typesafe.scalalogging.LazyLogging

case class FileControl(lines: Int, path: String, fileName: String, ext: String) extends LazyLogging {

  private var currentSuffix = 0
  private var currentLines = 0
  private var writer: Option[BufferedWriter] = Option(getWriter)

  def fn = (if (path.isEmpty) path else path + "/") + fileName + "_" + currentSuffix + "." + ext

  private def getWriter = new BufferedWriter(new FileWriter(fn, true))

  def append(newLine: String) = {

    logger.info("Appending new line to:" + fn)

    if (currentLines == lines) {
      close()
      currentSuffix = currentSuffix + 1
      currentLines = 0
      writer = Option(getWriter)
    }
    writer.foreach(_.append(newLine + "\n"))
    currentLines = currentLines + 1

  }

  def close() = writer.foreach(_.close())

  def secured(f: FileControl => Unit) = {
    try {
      f(this)
    } catch {
      case e: Exception =>
        logger.error(s"Error appending to file: $fn, Error: ${e.getMessage}")
    } finally {
      logger.info("Closing final stream")
      close()
    }

  }

}
