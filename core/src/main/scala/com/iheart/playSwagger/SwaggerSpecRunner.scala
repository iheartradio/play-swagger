package com.iheart.playSwagger

import java.nio.file.{Files, Paths, StandardOpenOption}

import play.api.libs.json.{JsSuccess, Json}

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  val (targetFile :: routesFile :: mappingsJson :: domainNameSpaceArgs) = args.toList
  private def fileArg = Paths.get(targetFile)
  import SwaggerMapping.format

  implicit val mappings: Seq[SwaggerMapping] = Json.parse(mappingsJson).validate[Seq[SwaggerMapping]] match {
    case JsSuccess(obj, _) ⇒
      obj
    case _ ⇒
      Seq()
  }
  private def swaggerJson = SwaggerSpecGenerator(domainNameSpaceArgs: _*).generate(routesFile).get.toString

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
