package com.iheart.playSwagger

import java.nio.file.{ Files, Paths, StandardOpenOption }

import scala.util.{ Failure, Success, Try }

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  val (targetFile :: routesFile :: domainNameSpaceArgs :: outputTransformersArgs :: swaggerV3String :: definitionNameTransformer :: Nil) = args.toList
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
    val nameTransformer = {
      Try(cl.loadClass(definitionNameTransformer).asSubclass(classOf[DefinitionNameTransformer]).newInstance()) match {
        case Failure(ex: ClassCastException) ⇒
          throw new IllegalArgumentException("Definition name transformer should be a subclass of com.iheart.playSwagger.DefinitionNameTransformer:" + definitionNameTransformer, ex)
        case Failure(ex) ⇒ throw new IllegalArgumentException("Could not create definition name transformer", ex)
        case Success(el) ⇒ el
      }
    }

    SwaggerSpecGenerator(
      domainModelQualifier,
      nameTransformer,
      outputTransformers = transformers,
      swaggerV3 = swaggerV3).generate(routesFile).get.toString
  }

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
