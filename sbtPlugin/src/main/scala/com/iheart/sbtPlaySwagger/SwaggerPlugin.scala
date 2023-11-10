package com.iheart.sbtPlaySwagger

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.*
import com.typesafe.sbt.web.Import.*
import sbt.Attributed.*
import sbt.Keys.*
import sbt.{AutoPlugin, *}

object SwaggerPlugin extends AutoPlugin {
  private lazy val SwaggerConfig: Configuration = config("play-swagger").hide
  private lazy val playSwaggerVersion: String = com.iheart.playSwagger.BuildInfo.version

  private object autoImport extends SwaggerKeys

  override def requires: Plugins = JavaAppPackaging

  override def trigger = noTrigger

  import autoImport.*

  override def projectConfigurations: Seq[Configuration] = Seq(SwaggerConfig)

  override def projectSettings: Seq[Setting[?]] = Seq(
    ivyConfigurations += SwaggerConfig,
    resolvers += Resolver.jcenterRepo,
    // todo: remove hardcoded org name using BuildInfo
    libraryDependencies += "io.github.play-swagger" %% "play-swagger" % playSwaggerVersion % SwaggerConfig,
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
      swaggerTarget.value.mkdirs()
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
        data((Runtime / fullClasspath).value) ++ update.value.select(configurationFilter(SwaggerConfig.name))
      runner.value.run(
        "com.iheart.playSwagger.SwaggerSpecRunner",
        swaggerClasspath,
        args,
        streams.value.log
      ).failed foreach (sys error _.getMessage)
      file
    }.value,
    Assets / unmanagedResourceDirectories += swaggerTarget.value,
    Compile / packageBin / mappings += swagger.value -> s"public/${swaggerFileName.value}", // include it in the unmanagedResourceDirectories in Assets doesn't automatically include it package
    Universal / packageBin := (Universal / packageBin).dependsOn(swagger).value,
    run := (Compile / run).dependsOn(swagger).evaluated,
    stage := stage.dependsOn(swagger).value
  )
}
