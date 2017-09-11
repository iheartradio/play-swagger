import sbt._

object Dependencies {
  object Versions {
    val play = "2.5.16"
    val specs2 = "3.9.5"
  }

  val playTest = Seq(
    "com.typesafe.play" %% "play-test" % Versions.play % Test)

  val playRoutesCompiler = Seq(
    "com.typesafe.play" %% "routes-compiler" % Versions.play)

  val playJson = Seq(
    "com.typesafe.play" %% "play-json" % Versions.play % "provided")

  val yaml = Seq(
    "org.yaml" % "snakeyaml" % "1.18")

  val test = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test")

}
