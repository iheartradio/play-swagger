import sbt.{Def, _}
import Keys._

object Publish {

  val coreSettings: Seq[Def.Setting[
    _ >: Option[URL] with Option[ScmInfo] with String with Seq[(String, URL)] with Boolean with List[Developer]
  ]] = Seq(
    organization := "io.github.play-swagger",
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("https://github.com/play-swagger/play-swagger")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/play-swagger/play-swagger"),
      "git@github.com:play-swagger/play-swagger.git"
    )),
    developers := List(
      Developer(
        "javakky",
        "Javakky",
        "javakky@pixiv.co.jp",
        url("https://twitter.com/javakky_P/")
      )
    ),
    Test / publishArtifact := false
  )

}
