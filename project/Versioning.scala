import sbt.Keys._
import sbt._

object Versioning {

  def writeVersionFile(path: String) = Def.task {
    val file = (resourceManaged in Compile).value / path
    IO.write(file, version.value.getBytes)
    Seq(file)
  }
}
