
organization in ThisBuild := "com.iheart"

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val root = project.in(file("."))
  .aggregate(playSwagger, sbtPlaySwagger)
  .settings(sourcesInBase := false)
  .settings(noPublishSettings: _*)

lazy val playSwagger = project.in(file("core"))
  .settings(Publish.coreSettings ++ Format.settings ++ Testing.settings)
  .settings(
    name := "play-swagger",
    libraryDependencies ++= Dependencies.playTest ++
      Dependencies.playRoutesCompiler ++
      Dependencies.playJson ++
      Dependencies.test ++
      Dependencies.yaml,
    scalaVersion := "2.11.11"
  )

lazy val sbtPlaySwagger = project.in(file("sbtPlugin"))
  .settings(Publish.sbtPluginSettings ++ Format.settings ++ ScriptedTesting.settings)
  .settings(
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2" % Provided),
    addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.3" % Provided))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "com.iheart.playSwagger",
    name := "sbt-play-swagger",
    description := "sbt plugin for play swagger spec generation",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    scripted := scripted.dependsOn(publishLocal in playSwagger).evaluated
  ).settings(
    scalaVersion := "2.12.2",
    sbtVersion in Global := "1.0.1",
    scalaCompilerBridgeSource := {
      val sv = appConfiguration.value.provider.id.version
      ("org.scala-sbt" % "compiler-interface" % sv % "component").sources
    }
  )
