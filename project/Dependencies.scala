import sbt._
import Keys._

object Dependencies {
  object Versions {
    val specs2 = "3.8.9"
    val play = Def.setting {
      scalaBinaryVersion.value match {
        case "2.12" => "2.6.1"
        case _ => "2.5.14"
      }
    }
  }

  val playTest = Def.setting {
    Seq(
      "com.typesafe.play" %% "play-test" % Versions.play.value % Test
    )
  }

  val playRoutesCompiler = Def.setting {
    Seq(
      "com.typesafe.play" %% "routes-compiler" % Versions.play.value
    )
  }

  val playJson = Def.setting {
    Seq(
      "com.typesafe.play" %% "play-json" % Versions.play.value % "provided"
    )
  }

  val yaml = Seq(
    "org.yaml" % "snakeyaml" % "1.16"
  )

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

}
