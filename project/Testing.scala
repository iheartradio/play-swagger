import org.scoverage.coveralls.Imports.CoverallsKeys._
import sbt._
import sbt.Keys._

object Testing {


  lazy val settings = Seq(
    coverallsToken := Some("tVYHvi1dwcXx3XzTnEOCLjCneOei9wraz"),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    testOptions in Test := Seq(Tests.Argument(TestFrameworks.Specs2, "-xonly"))
  )


}
