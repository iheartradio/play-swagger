import sbt.Keys._
import sbt.{Def, _}

object Versioning {

  def writeVersionFile(path: String): Def.Initialize[Task[Seq[File]]] = Def.task {
    val file = (Compile / resourceManaged).value / path
    IO.write(file, version.value.getBytes)
    Seq(file)
  }
}
