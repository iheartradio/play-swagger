ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
ThisBuild / publish / skip := true
ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0",
  "com.sandinh" %% "scala-rewrites" % "1.1.0-M1",
  "net.pixiv" %% "scalafix-pixiv-rule" % "4.5.3",
  "com.github.xuwei-k" %% "scalafix-rules" % "0.3.1",
  "com.github.jatcwang" %% "scalafix-named-params" % "0.2.3"
)

addCommandAlias(
  "publishForExample",
  ";set ThisBuild / version := \"0.0.1-EXAMPLE\"; +publishLocal"
)

lazy val scalaV = "2.12.18"

lazy val root = project.in(file("."))
  .aggregate(playSwagger, sbtPlaySwagger)
  .settings(
    sonatypeProfileName := "io.github.play-swagger",
    publish / skip := true,
    sourcesInBase := false,
    scalaVersion := scalaV
  )

lazy val playSwagger = project.in(file("core"))
  .enablePlugins(GitBranchPrompt)
  .settings(
    publish / skip := false,
    Publish.coreSettings,
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
        "org.scalameta" %% "scalameta" % "4.8.12",
        "net.steppschuh.markdowngenerator" % "markdowngenerator" % "1.3.1.1",
        "joda-time" % "joda-time" % "2.12.5" % Test,
        "com.google.errorprone" % "error_prone_annotations" % "2.23.0" % Test
      ),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    addCompilerPlugin("com.github.takezoe" %% "runtime-scaladoc-reader" % "1.0.3"),
    scalaVersion := scalaV,
    crossScalaVersions := Seq(scalaVersion.value, "2.13.12"),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Wunused")
      case _ => Seq("-Xlint:unused")
    }) ++ Seq(
      "-deprecation",
      "-feature",
      "-Ypatmat-exhaust-depth",
      "40",
      "-P:semanticdb:synthetics:on"
    )
  )

lazy val sbtPlaySwagger = project.in(file("sbtPlugin"))
  .enablePlugins(GitBranchPrompt)
  .settings(
    publish / skip := false,
    Publish.coreSettings,
    addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.25" % Provided),
    addSbtPlugin("com.github.sbt" %% "sbt-web" % "1.5.2" % Provided)
  )
  .enablePlugins(BuildInfoPlugin, SbtPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "com.iheart.playSwagger",
    name := "sbt-play-swagger",
    description := "sbt plugin for play swagger spec generation",
    sbtPlugin := true,
    scalaVersion := scalaV,
    scripted := scripted.dependsOn(playSwagger / publishLocal).evaluated,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Wunused")
      case _ => Seq("-Xlint:unused")
    }) ++ Seq(
      "-deprecation",
      "-feature",
      "-Ypatmat-exhaust-depth",
      "40",
      "-P:semanticdb:synthetics:on"
    )
  )
