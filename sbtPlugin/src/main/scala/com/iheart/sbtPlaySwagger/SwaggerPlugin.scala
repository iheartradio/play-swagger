package com.iheart.sbtPlaySwagger

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.web.Import._
import sbt.Attributed._
import sbt.Keys._
import sbt.{AutoPlugin, _}

object SwaggerPlugin extends AutoPlugin {
  lazy val SwaggerConfig: Configuration = config("play-swagger").hide
  lazy val playSwaggerVersion: String = com.iheart.playSwagger.BuildInfo.version

  object autoImport extends SwaggerKeys

  override def requires: Plugins = JavaAppPackaging

  override def trigger = noTrigger

  import autoImport._

  override def projectConfigurations: Seq[Configuration] = Seq(SwaggerConfig)

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += SwaggerConfig,
    resolvers += Resolver.jcenterRepo,
    // todo: remove hardcoded org name using BuildInfo
    libraryDependencies += "com.iheart" %% "play-swagger" % playSwaggerVersion % SwaggerConfig,
    swaggerDomainNameSpaces := Seq(),
    swaggerV3 := false,
    swaggerTarget := target.value / "swagger",
    swaggerFileName := "swagger.json",
    swaggerRoutesFile := "routes",
    swaggerOutputTransformers := Seq("com.iheart.playSwagger.ParametricTypeNamesTransformer"),
    swaggerAPIVersion := version.value,
    swaggerPrettyJson := false,
    swaggerPlayJava := false,
    swaggerNamingStrategy := "none",
    swaggerOperationIdNamingFully := false,
    embedScaladoc := false,
    swagger := Def.task[File] {
      (swaggerTarget.value).mkdirs()
      val file = swaggerTarget.value / swaggerFileName.value
      IO.delete(file)
      val args: Seq[String] = file.absolutePath :: swaggerRoutesFile.value ::
        swaggerDomainNameSpaces.value.mkString(",") ::
        swaggerOutputTransformers.value.mkString(",") ::
        swaggerV3.value.toString ::
        swaggerAPIVersion.value ::
        swaggerPrettyJson.value.toString ::
        swaggerPlayJava.value.toString ::
        swaggerNamingStrategy.value ::
        swaggerOperationIdNamingFully.value.toString ::
        embedScaladoc.value.toString ::
        Nil
      val swaggerClasspath =
        data((fullClasspath in Runtime).value) ++ update.value.select(configurationFilter(SwaggerConfig.name))
      runner.value.run(
        "com.iheart.playSwagger.SwaggerSpecRunner",
        swaggerClasspath,
        args,
        streams.value.log
      ).failed foreach (sys error _.getMessage)
      file
    }.value,
    unmanagedResourceDirectories in Assets += swaggerTarget.value,
    mappings in (Compile, packageBin) += (swagger.value) → s"public/${swaggerFileName.value}", // include it in the unmanagedResourceDirectories in Assets doesn't automatically include it package
    packageBin in Universal := (packageBin in Universal).dependsOn(swagger).value,
    run := (run in Compile).dependsOn(swagger).evaluated,
    stage := stage.dependsOn(swagger).value
  )
}
