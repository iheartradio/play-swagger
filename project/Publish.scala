import sbt._, Keys._
import bintray.BintrayKeys._


object Publish {

  val bintraySettings = Seq(
    bintrayOrganization := Some("iheartradio"),
    bintrayPackageLabels := Seq("play-framework", "swagger", "rest-api", "API", "documentation")
  )

  val publishingSettings = Seq(
    publishMavenStyle := true,
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("http://iheartradio.github.io/play-swagger")),
    scmInfo := Some(ScmInfo(url("https://github.com/iheartradio/play-swagger"),
      "git@github.com:iheartradio/play-swagger.git")),
    pomIncludeRepository := { _ => false },
    publishArtifact in Test := false
  )

  val settings = bintraySettings ++ publishingSettings
}
