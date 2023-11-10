import sbt._

object Dependencies {
  object Versions {
    val play = "2.8.21"
    val playJson = "2.10.3"
    val specs2 = "4.20.3"
    val enumeratum = "1.7.3"
    val refined = "0.11.0"
  }

  val playTest: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-test" % Versions.play % Test
  )

  val playRoutesCompiler: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "routes-compiler" % Versions.play
  )

  val playJson: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-json" % Versions.playJson % "provided"
  )

  val yaml: Seq[ModuleID] = Seq(
    "org.yaml" % "snakeyaml" % "2.2"
  )

  val enumeratum: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum" % Versions.enumeratum % Test
  )

  val refined: Seq[ModuleID] = Seq(
    "eu.timepit" %% "refined" % Versions.refined % Test
  )

  val test: Seq[ModuleID] = Seq(
    "org.specs2" %% "specs2-core" % Versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % Versions.specs2 % "test"
  )

}
