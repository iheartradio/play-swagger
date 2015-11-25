
organization in ThisBuild := "com.iheart"

name := "play-swagger"

version := "0.1.6-LOCAL"

resolvers +=  Resolver.bintrayRepo("scalaz", "releases")

scalaVersion in ThisBuild := "2.11.7"

libraryDependencies ++= Dependencies.playJson ++ Dependencies.test ++ Dependencies.yaml

Publish.settings

lazy val playSwagger = project in file(".")

Testing.settings

Format.settings
