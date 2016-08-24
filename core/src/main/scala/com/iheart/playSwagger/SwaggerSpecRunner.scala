package com.iheart.playSwagger

import java.nio.file.{Files, Paths, StandardOpenOption}

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  private def fileArg = Paths.get(args.head)
  private def domainNameSpaceArgs = args.tail
  private def swaggerJson = SwaggerSpecGenerator(domainNameSpaceArgs: _*).generate().get.toString

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
