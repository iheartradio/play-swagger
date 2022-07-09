import org.scoverage.coveralls.Imports.CoverallsKeys._
import sbt._
import sbt.Keys._

object Testing {

  lazy val settings = Seq(
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

}
