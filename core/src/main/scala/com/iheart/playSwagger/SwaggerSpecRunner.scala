package com.iheart.playSwagger

import java.nio.file.{ Files, Paths, StandardOpenOption }

import scala.util.{ Failure, Success, Try }

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  val (targetFile :: routesFile :: domainNameSpaceArgs :: outputTransformersArgs :: swaggerV3String :: swaggerDefinitionsCaseType :: Nil) = args.toList
  private def fileArg = Paths.get(targetFile)
  private def swaggerJson = {
    val swaggerV3 = java.lang.Boolean.parseBoolean(swaggerV3String)
    val domainModelQualifier = PrefixDomainModelQualifier(domainNameSpaceArgs.split(","): _*)
    val transformersStrs: Seq[String] = if (outputTransformersArgs.isEmpty) Seq() else outputTransformersArgs.split(",")
    val transformers = transformersStrs.map { clazz ⇒
      Try(cl.loadClass(clazz).asSubclass(classOf[OutputTransformer]).newInstance()) match {
        case Failure(ex: ClassCastException) ⇒
          throw new IllegalArgumentException("Transformer should be a subclass of com.iheart.playSwagger.OutputTransformer:" + clazz, ex)
        case Failure(ex) ⇒ throw new IllegalArgumentException("Could not create transformer", ex)
        case Success(el) ⇒ el
      }
    }
    val caseType = swaggerDefinitionsCaseType match {
      case "camelCase"  ⇒ CamelCase
      case "snakeCases" ⇒ SnakeCase
      case _            ⇒ CamelCase
    }

    SwaggerSpecGenerator(
      domainModelQualifier,
      caseType,
      outputTransformers = transformers,
      swaggerV3 = swaggerV3).generate(routesFile).get.toString
  }

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
