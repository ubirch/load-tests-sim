package com.ubirch.models

import java.io.{ File, FilenameFilter }

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

class TestDataFileFilter(fileName: String, suffixes: List[String], ext: String) extends FilenameFilter {
  override def accept(file: File, name: String): Boolean = {
    val basic = name.startsWith(fileName) && name.endsWith(ext)
    val extra = if (suffixes.nonEmpty) {
      suffixes
        .map(s => s + "." + ext)
        .map(s => name.contains(s))
        .exists(x => x)
    } else {
      true
    }

    basic && extra
  }
}

case class ReadFileControl(path: String, directory: String, fileName: String, suffixes: List[String], ext: String) extends LazyLogging {

  def read[B](func: String => B) = {
    val _path = path + "/" + directory
    val files = new File(_path).listFiles(new TestDataFileFilter(fileName, suffixes, ext))
    val filesAsList = Option(files).map(_.toList).getOrElse(Nil)
    if (filesAsList.isEmpty) {
      logger.info("No files to read from.")
    }
    filesAsList.flatMap { f =>
      val re = Source.fromFile(f)
      try {
        re.getLines().map(func).toList
      } catch {
        case e: Exception =>
          logger.error("Error reading file" + e.getMessage)
          throw e
      } finally {
        re.close()
      }
    }
  }

}
