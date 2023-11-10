import org.scoverage.coveralls.Imports.CoverallsKeys._
import sbt.{Def, _}
import sbt.Keys._

object Testing {

  lazy val settings: Seq[Def.Setting[Task[Seq[String]]]] = Seq(
    Test / scalacOptions ++= Seq("-Yrangepos")
  )

}
