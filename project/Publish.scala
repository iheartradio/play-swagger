import com.jsuereth.sbtpgp.PgpKeys
import xerial.sbt.Sonatype.autoImport._
import sbt._, Keys._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseTransformations._

object Publish {

  val coreSettings = Seq(
    organization in ThisBuild := "com.iheart",
    publishMavenStyle := true,
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("http://iheartradio.github.io/play-swagger")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/iheartradio/play-swagger"),
      "git@github.com:iheartradio/play-swagger.git"
    )),
    developers := List(
      Developer(
        "kailuowang",
        "Kailuo Wang",
        "kailuo.wang@gmail.com",
        url("https://kailuowang.com")
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    pomIncludeRepository := { _ ⇒ false },
    publishArtifact in Test := false,
    releaseCrossBuild := true,
    publishTo := sonatypePublishToBundle.value,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      releaseStepCommandAndRemaining("+clean"),
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

}
