package com.ubirch.models

import java.io.{ BufferedWriter, File, FileWriter }

import com.typesafe.scalalogging.LazyLogging

case class WriteFileControl(lines: Int, path: String, directory: String, fileName: String, ext: String) extends LazyLogging {

  private var currentSuffix = 0
  private var currentLines = 0
  private var writer: Option[BufferedWriter] = Option(getWriter)

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

  def secured(f: WriteFileControl => Unit) = {
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

  def fn = {
    val p = if (path.isEmpty) path else path + "/"
    val d = if (directory.isEmpty) directory else directory + "/"
    ensureDirectory(p + d)
    p + d + fileName + "_" + currentSuffix + "." + ext
  }

  def ensureDirectory(directory: String) = {
    if (directory.nonEmpty) {
      val file = new File(directory)
      if (!file.exists()) {
        logger.info("Creating directory ...")
        file.mkdir()
      } else {
        false
      }
    } else {
      true
    }
  }

  def close() = writer.foreach(_.close())

  private def getWriter = new BufferedWriter(new FileWriter(fn, true))

}
