import sbt._, Keys._
import bintray.BintrayKeys._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseTransformations._
object Publish {

  val coreSettings = Seq(
    bintrayOrganization := Some("iheartradio"),
    bintrayPackageLabels := Seq("play-framework", "swagger", "rest-api", "API", "documentation"),
    publishMavenStyle := true,
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("http://iheartradio.github.io/play-swagger")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/iheartradio/play-swagger"),
      "git@github.com:iheartradio/play-swagger.git")),
    pomIncludeRepository := { _ ⇒ false },
    publishArtifact in Test := false,
    releaseProcess :=  Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      releaseStepCommandAndRemaining("+clean"),
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publish"),
      setNextVersion,
      commitNextVersion,
      pushChanges)
  )
}
