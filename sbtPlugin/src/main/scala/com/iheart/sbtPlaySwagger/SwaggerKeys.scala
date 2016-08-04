package com.iheart.sbtPlaySwagger

import java.io.File

import sbt.{SettingKey, TaskKey}

trait SwaggerKeys {
  val swagger = TaskKey[File]("swagger", "generates the swagger API documentation")
  val swaggerDomainNameSpaces = SettingKey[Seq[String]]("swaggerDomainNameSpaces", "swagger domain namespaces for model classes")
  val swaggerTarget = SettingKey[String]("swaggerTarget", "the location of the swagger documentation in your packaged app.")
}