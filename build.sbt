import org.scoverage.coveralls.Imports.CoverallsKeys._

organization in ThisBuild := "com.iheart"

name := "play-swagger"

resolvers +=  Resolver.bintrayRepo("scalaz", "releases")

scalaVersion in ThisBuild := "2.11.7"

libraryDependencies ++= Dependencies.playJson ++ Dependencies.test ++ Dependencies.yaml

Publish.settings

lazy val playSwagger = project in file(".")

coverallsToken := Some("tVYHvi1dwcXx3XzTnEOCLjCneOei9wraz")

scalariformSettings
