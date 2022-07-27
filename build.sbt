organization in ThisBuild := "com.iheart"

scalafixDependencies in ThisBuild ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0",
  "net.pixiv" %% "scalafix-pixiv-rule" % "2.2.0"
)

lazy val noPublishSettings = Seq(
  skip in publish := true,
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val scalaV = "2.12.16"

lazy val root = project.in(file("."))
  .aggregate(playSwagger, sbtPlaySwagger)
  .settings(
    Publish.coreSettings,
    sourcesInBase := false,
    noPublishSettings,
    scalaVersion := scalaV
  )

lazy val playSwagger = project.in(file("core"))
  .settings(
    Publish.coreSettings,
    Format.settings,
    Testing.settings,
    name := "play-swagger",
    libraryDependencies ++= Dependencies.playTest ++
      Dependencies.playRoutesCompiler ++
      Dependencies.playJson ++
      Dependencies.enumeratum ++
      Dependencies.refined ++
      Dependencies.test ++
      Dependencies.yaml,
    scalaVersion := scalaV,
    crossScalaVersions := Seq(scalaVersion.value, "2.13.8"),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Wunused")
      case _ => Seq("-Xlint:unused")
    })
  )

lazy val sbtPlaySwagger = project.in(file("sbtPlugin"))
  .settings(
    Publish.coreSettings,
    Format.settings,
    addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.17" % Provided),
    addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.4.4" % Provided)
  )
  .enablePlugins(BuildInfoPlugin, SbtPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "com.iheart.playSwagger",
    name := "sbt-play-swagger",
    description := "sbt plugin for play swagger spec generation",
    sbtPlugin := true,
    scalaVersion := scalaV,
    scripted := scripted.dependsOn(publishLocal in playSwagger).evaluated,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Wunused")
      case _ => Seq("-Xlint:unused")
    })
  )
