package com.iheart.sbtPlaySwagger

import java.io.File

import sbt.{ SettingKey, TaskKey }

trait SwaggerKeys {
  val swagger = TaskKey[File]("swagger", "generates the swagger API documentation")
  val swaggerDomainNameSpaces = SettingKey[Seq[String]]("swaggerDomainNameSpaces", "swagger domain namespaces for model classes")
  val swaggerTarget = SettingKey[File]("swaggerTarget", "the location of the swagger documentation in your packaged app.")
  val swaggerFileName = SettingKey[String]("swaggerFileName", "the swagger filename the swagger documentation in your packaged app.")
  val swaggerRoutesFile = SettingKey[String]("swaggerRoutesFile", "the root routes file with which play-swagger start to parse")
  val swaggerOutputTransformers = SettingKey[Seq[String]]("swaggerOutputTransformers", "list of output transformers for processing swagger file")
  val swaggerV3 = SettingKey[Boolean]("swaggerV3", "whether to to produce output compatible with Swagger 3 (also knwon as OpenAPI 3)")
  val swaggerDescriptionFile = SettingKey[Option[File]]("swaggerDescriptionFile", "The location of a java properties file to supply descriptions for types and parameters.")
  val envOutputTransformer = "com.iheart.playSwagger.EnvironmentVariablesTransformer"
}
