package com.ubirch.models

import java.io.{ File, FilenameFilter }

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

class TestDataFile(fileName: String, ext: String) extends FilenameFilter {
  override def accept(file: File, name: String): Boolean = {
    name.startsWith(fileName) && name.endsWith(ext)
  }
}

case class ReadFileControl(path: String, directory: String, fileName: String, ext: String) extends LazyLogging {

  def read[B](func: String => B) = {
    val _path = path + "/" + directory
    val files = new File(_path).listFiles(new TestDataFile(fileName, ext)).toList
    files.foreach { f =>
      val re = Source.fromFile(f)
      try {
        re.getLines().foreach(func)
      } catch {
        case e: Exception =>
          logger.error("Error reading file" + e.getMessage)
      } finally {
        re.close()
      }
    }
  }

}
