
organization in ThisBuild := "com.iheart"

resolvers +=  Resolver.bintrayRepo("scalaz", "releases")

scalaVersion in ThisBuild := "2.11.7"

libraryDependencies ++= Dependencies.playJson ++ Dependencies.test ++ Dependencies.yaml

Publish.settings

lazy val playSwagger = project in file(".")

Testing.settings

Format.settings
