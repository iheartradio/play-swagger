package com.iheart.playSwagger

import java.nio.file.{Files, Paths, StandardOpenOption}

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  val (targetFile :: routesFile :: domainNameSpaceArgs) = args.toList
  private def fileArg = Paths.get(targetFile)
  private def swaggerJson = SwaggerSpecGenerator(domainNameSpaceArgs: _*).generate(routesFile).get.toString

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
