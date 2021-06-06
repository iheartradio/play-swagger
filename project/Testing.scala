import sbt.Keys._
import sbt._

object Testing {

  lazy val settings = Seq(
    Test / scalacOptions ++= Seq("-Yrangepos"))

}
