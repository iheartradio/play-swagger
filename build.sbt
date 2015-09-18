
organization in ThisBuild := "com.iheart"

name := "play-swagger"

version in ThisBuild := "0.1.0-RELEASE"

scalaVersion in ThisBuild := "2.11.7"

libraryDependencies ++= Dependencies.playJson ++ Dependencies.test ++ Dependencies.yaml


licenses += ("Apache-2.0", url("http://www.apache.org/licenses/"))

bintrayOrganization := Some("iheartradio")

bintrayPackageLabels := Seq("play-framework", "swagger", "play")
