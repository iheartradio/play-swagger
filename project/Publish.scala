import com.jsuereth.sbtpgp.PgpKeys
import xerial.sbt.Sonatype.autoImport._
import sbt._, Keys._

object Publish {

  val coreSettings = Seq(
    ThisBuild / organization  := "com.iheart",
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
    pomIncludeRepository := { _ ⇒ false },
    Test / publishArtifact := false,
    publishTo := sonatypePublishToBundle.value
  )

}
