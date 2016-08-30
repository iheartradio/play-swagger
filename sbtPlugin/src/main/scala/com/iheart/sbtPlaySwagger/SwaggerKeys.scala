package com.iheart.sbtPlaySwagger

import java.io.File

import sbt.{SettingKey, TaskKey}

trait SwaggerKeys {
  val swagger = TaskKey[File]("swagger", "generates the swagger API documentation")
  val swaggerDomainNameSpaces = SettingKey[Seq[String]]("swaggerDomainNameSpaces", "swagger domain namespaces for model classes")
  val swaggerTarget = SettingKey[File]("swaggerTarget", "the location of the swagger documentation in your packaged app.")
  val swaggerFileName = SettingKey[String]("swaggerFileName", "the swagger filename the swagger documentation in your packaged app.")
  val swaggerRoutesFile = SettingKey[String]("swaggerRoutesFile", "the root routes file with which play-swagger start to parse")
  val swaggerMappings = SettingKey[Seq[SwaggerMapping]]("swaggerMappings", "overrides of the default type mappings")
}
