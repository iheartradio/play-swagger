package com.iheart.playSwagger

import java.nio.file.{Files, Paths, StandardOpenOption}

import scala.util.{Success, Failure, Try}

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  val (targetFile :: routesFile :: domainNameSpaceArgs :: outputTransformersArgs :: Nil) = args.toList
  private def fileArg = Paths.get(targetFile)
  private def swaggerJson = {
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
    SwaggerSpecGenerator(domainModelQualifier, outputTransformers = transformers).generate(routesFile).get.toString
  }

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
