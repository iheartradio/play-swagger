package com.iheart.sbtPlaySwagger

import com.google.common.base.Charsets.UTF_8
import com.google.common.io.Resources.{getResource, toString ⇒ read}
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Attributed._
import sbt.Keys._
import sbt.{AutoPlugin, _}

object SwaggerPlugin extends AutoPlugin {
  lazy val swaggerConfig = config("play-swagger").hide
  lazy val playSwaggerVersion = com.iheart.playSwagger.BuildInfo.version

  object autoImport extends SwaggerKeys

  override def requires: Plugins = JavaAppPackaging

  override def trigger = noTrigger

  import autoImport._

  override def projectConfigurations: Seq[Configuration] = Seq(swaggerConfig)

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += swaggerConfig,
    libraryDependencies += "com.iheart" %% "play-swagger" % playSwaggerVersion % swaggerConfig,
    swaggerDomainNameSpaces := Seq(),
    swaggerTarget := "public/swagger.json",
    swagger <<= Def.task[File] {
      (target.value / "swagger").mkdirs()
      val file = target.value / "swagger" / "swagger.json"
      IO.delete(file)
      val args = file.absolutePath +: swaggerDomainNameSpaces.value
      val swaggerClasspath = data((fullClasspath in Runtime).value) ++ update.value.select(configurationFilter(swaggerConfig.name))
      toError(runner.value.run("com.iheart.playSwagger.SwaggerSpecRunner", swaggerClasspath, args, streams.value.log))
      file
    },
    mappings in (Compile, packageBin) += (target.value / "swagger" / "swagger.json") → swaggerTarget.value,
    packageBin in Universal <<= (packageBin in Universal).dependsOn(swagger),
    stage <<= stage.dependsOn(swagger)
  )
}

