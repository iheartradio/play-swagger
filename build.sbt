
lazy val commonSettings = Seq(
  organization := "com.iheart",
  scalaVersion := "2.11.7",
  scalacOptions in (Compile, compile) ++= Seq(
    "-encoding", "UTF-8",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xlint", // recommended additional warnings
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    "-Ywarn-inaccessible",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen"
  )
) ++
  Publish.settings ++
  Testing.settings ++
  Format.settings

lazy val playSwagger = (project in file("."))
  .dependsOn(macros)
  .settings(commonSettings:_*)
  .settings(
    name := "play-swagger",
    resolvers +=  Resolver.bintrayRepo("scalaz", "releases"),
    libraryDependencies ++=
      Dependencies.playTest ++
      Dependencies.playRoutesCompiler ++
      Dependencies.playJson ++
      Dependencies.test ++
      Dependencies.yaml ++ Seq(
        "com.chuusai" %% "shapeless" % "2.3.0" % "provided"
      )
  )


lazy val macros = project
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "macro-compat" % "1.1.1",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "com.typesafe.play" %% "routes-compiler" % "2.4.6",
      "com.chuusai" %% "shapeless" % "2.3.0" % "provided"
    )
  )
  .settings(commonSettings:_*)
