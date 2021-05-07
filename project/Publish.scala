import com.typesafe.sbt.pgp.PgpKeys
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
      "git@github.com:iheartradio/play-swagger.git")),
    developers := List(
      Developer(
        "kailuowang",
        "Kailuo Wang",
        "kailuo.wang@gmail.com",
        url("https://kailuowang.com")
      )
    ),
    pomIncludeRepository := { _ ⇒ false },
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("Snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("Releases" at nexus + "service/local/staging/deploy/maven2")
    },
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges))

}
