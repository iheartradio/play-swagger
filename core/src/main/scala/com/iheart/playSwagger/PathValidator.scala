package com.iheart.playSwagger

import java.nio.file.{InvalidPathException, Paths}

object PathValidator {
  def isValid(path: String): Boolean = {
    try {
      Paths.get(path)
      true
    } catch {
      case _: InvalidPathException | _: NullPointerException => false
    }
  }
}
