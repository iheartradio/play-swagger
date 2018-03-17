package com.iheart.playSwagger

import java.io.File
import java.nio.file.{ Files, Paths, StandardOpenOption }

import scala.util.{ Failure, Success, Try }

object SwaggerSpecRunner extends App {
  implicit def cl = getClass.getClassLoader

  val (targetFile :: routesFile :: domainNameSpaceArgs :: outputTransformersArgs :: swaggerV3String :: Nil) = args.take(5).toList
  parseOptions(new SwaggerSpecRunnerOptions, args.drop(5).toList) match {
    case SwaggerSpecRunnerOptions(Some(descriptionFile)) ⇒
      Descriptions.useDescriptionFile(new File(descriptionFile))
    case _ ⇒ // use default description provider when option is not supplied
  }

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
    SwaggerSpecGenerator(
      domainModelQualifier,
      outputTransformers = transformers,
      swaggerV3 = swaggerV3).generate(routesFile).get.toString
  }

  Files.write(fileArg, swaggerJson.getBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)

  def parseOptions(options: SwaggerSpecRunnerOptions, list: List[String]): SwaggerSpecRunnerOptions = {
    list match {
      case Nil ⇒ options
      case "--description-file" :: v :: others ⇒
        val newOptions = options.copy(descriptionFile = Some(v))
        parseOptions(newOptions, others)
      case s :: others ⇒
        // discard normal argument, they are taken in the beginning and should not exist
        parseOptions(options, others)
    }
  }

  case class SwaggerSpecRunnerOptions(
    descriptionFile: Option[String] = None)
}
