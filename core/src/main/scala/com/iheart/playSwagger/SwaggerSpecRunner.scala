package com.iheart.playSwagger

import java.nio.file.{Files, Paths, StandardOpenOption}

import scala.util.{Failure, Success, Try}

import play.api.libs.json.{JsValue, Json}

object SwaggerSpecRunner extends App {
  implicit def cl: ClassLoader = getClass.getClassLoader

  val targetFile :: routesFile :: domainNameSpaceArgs :: outputTransformersArgs :: swaggerV3String :: apiVersion :: swaggerPrettyJson :: swaggerPlayJavaString :: namingStrategy :: operationIdNamingFullyString :: Nil =
    args.toList
  private def fileArg = Paths.get(targetFile)
  private def swaggerJson = {
    val swaggerV3 = java.lang.Boolean.parseBoolean(swaggerV3String)
    val swaggerOperationIdNamingFully = java.lang.Boolean.parseBoolean(operationIdNamingFullyString)
    val swaggerPlayJava = java.lang.Boolean.parseBoolean(swaggerPlayJavaString)
    val domainModelQualifier = PrefixDomainModelQualifier(domainNameSpaceArgs.split(","): _*)
    val transformersStrs: Seq[String] = if (outputTransformersArgs.isEmpty) Seq() else outputTransformersArgs.split(",")
    val transformers = transformersStrs.map { clazz ⇒
      Try(cl.loadClass(clazz).asSubclass(classOf[OutputTransformer]).newInstance()) match {
        case Failure(ex: ClassCastException) ⇒
          throw new IllegalArgumentException(
            "Transformer should be a subclass of com.iheart.playSwagger.OutputTransformer:" + clazz,
            ex
          )
        case Failure(ex) ⇒ throw new IllegalArgumentException("Could not create transformer", ex)
        case Success(el) ⇒ el
      }
    }
    val swaggerSpec: JsValue = SwaggerSpecGenerator(
      NamingStrategy.from(namingStrategy),
      domainModelQualifier,
      outputTransformers = transformers,
      swaggerV3 = swaggerV3,
      swaggerPlayJava = swaggerPlayJava,
      apiVersion = Some(apiVersion),
      operationIdFully = swaggerOperationIdNamingFully
    ).generate(routesFile).get

    if (swaggerPrettyJson.toBoolean) Json.prettyPrint(swaggerSpec)
    else swaggerSpec.toString
  }

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
}
