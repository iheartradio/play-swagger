
organization in ThisBuild := "com.iheart"

name := "play-swagger"

resolvers +=  Resolver.bintrayRepo("scalaz", "releases")

scalaVersion in ThisBuild := "2.11.7"

libraryDependencies ++=
  Dependencies.playTest ++
  Dependencies.playRoutesCompiler ++
  Dependencies.playJson ++
  Dependencies.test ++
  Dependencies.yaml

Publish.settings

lazy val playSwagger = project in file(".")

Testing.settings

Format.settings

