organization in ThisBuild := "com.iheart"

ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0",
  "net.pixiv" %% "scalafix-pixiv-rule" % "4.5.1"
)

addCommandAlias(
  "publishForExample",
  ";set ThisBuild / version := \"0.0.1-EXAMPLE\"; +publishLocal"
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val scalaV = "2.12.18"

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
      Dependencies.yaml ++ Seq(
        "com.github.takezoe" %% "runtime-scaladoc-reader" % "1.0.3",
        "org.scalameta" %% "scalameta" % "4.7.8",
        "net.steppschuh.markdowngenerator" % "markdowngenerator" % "1.3.1.1",
        "joda-time" % "joda-time" % "2.12.5" % Test,
        "com.google.errorprone" % "error_prone_annotations" % "2.19.1" % Test
      ),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    addCompilerPlugin("com.github.takezoe" %% "runtime-scaladoc-reader" % "1.0.3"),
    scalaVersion := scalaV,
    crossScalaVersions := Seq(scalaVersion.value, "2.13.10"),
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
