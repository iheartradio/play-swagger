package com.iheart.playSwagger.generator

import java.io.{IOException, InputStream}

import scala.io.Source
import scala.util.{Failure, Try}

object ResourceReader {

  def read(resource: String)(implicit cl: ClassLoader): Try[List[String]] = {
    val r = cl.getResourceAsStream(resource)
    if (r != null) {
      read(r)
    } else {
      Failure(new IOException(s"Failed to find resource '$resource'"))
    }
  }

  def read(stream: InputStream): Try[List[String]] = Try {
    Source.fromInputStream(stream).getLines.toList
  }

}
