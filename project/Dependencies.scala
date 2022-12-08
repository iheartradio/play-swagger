import sbt._

object Dependencies {
  object Versions {
    val play = "2.8.16"
    val playJson = "2.8.2"
    val specs2 = "4.16.1"
    val enumeratum = "1.7.2"
    val refined = "0.10.1"
  }

  val playTest = Seq(
    "com.typesafe.play" %% "play-test" % Versions.play % Test
  )

  val playRoutesCompiler = Seq(
    "com.typesafe.play" %% "routes-compiler" % Versions.play
  )

  val playJson = Seq(
    "com.typesafe.play" %% "play-json" % Versions.playJson % "provided"
  )

  val yaml = Seq(
    "org.yaml" % "snakeyaml" % "1.32"
  )

  val enumeratum = Seq(
    "com.beachape" %% "enumeratum" % Versions.enumeratum % Test
  )

  val refined = Seq(
    "eu.timepit" %% "refined" % Versions.refined % Test
  )

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

}
