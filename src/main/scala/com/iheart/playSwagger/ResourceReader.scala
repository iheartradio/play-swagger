package com.iheart.playSwagger

import java.io.InputStream

object ResourceReader {
  def read(resource: String)(implicit cl: ClassLoader): List[String] = {
    read(cl.getResourceAsStream(resource))
  }

  def read(stream: InputStream): List[String] = {
    scala.io.Source.fromInputStream(stream).getLines.toList
  }

}
