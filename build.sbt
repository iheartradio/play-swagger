
organization in ThisBuild := "com.iheart"

resolvers +=  Resolver.bintrayRepo("scalaz", "releases")


lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val root = project.in(file("."))
  .enablePlugins(CrossPerProjectPlugin)
  .aggregate(playSwagger, sbtPlaySwagger)
  .settings(
    noPublishSettings,
    sourcesInBase := false,
    scalaVersion := "2.11.11",
    releaseCrossBuild := false
  )

lazy val playSwagger = project.in(file("core"))
  .enablePlugins(CrossPerProjectPlugin)
  .settings(Publish.coreSettings ++ Format.settings ++ Testing.settings)
  .settings(
    name := "play-swagger",
    libraryDependencies ++= Dependencies.playTest.value ++
      Dependencies.playRoutesCompiler.value ++
      Dependencies.playJson.value ++
      Dependencies.test ++
      Dependencies.yaml,
    scalaVersion := "2.11.11",
    crossScalaVersions := Seq("2.11.11", "2.12.2")
  )

lazy val sbtPlaySwagger = project.in(file("sbtPlugin"))
  .enablePlugins(CrossPerProjectPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(Publish.sbtPluginSettings ++ Format.settings ++ ScriptedTesting.settings)
  .settings(
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6" % Provided),
    addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.3.0" % Provided),
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "com.iheart.playSwagger",
    name := "sbt-play-swagger",
    description := "sbt plugin for play swagger spec generation",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq("2.10.6"),
    scripted := scripted.dependsOn(publishLocal in playSwagger).evaluated
  )
