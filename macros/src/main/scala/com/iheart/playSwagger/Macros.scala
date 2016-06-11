package com.iheart.playSwagger

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import play.routes.compiler._

import scala.io.Source
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

sealed trait Routing extends Product with Serializable
case class RouteFile(prefix: String, file: String, routes: List[Routing]) extends Routing
case class RouteDef(route: Route) extends Routing

object Macros {

  def apply(routes: String): Seq[Route] = macro routesImpl

  def all(routesFile: String): Routing = macro allImpl

  def allImpl(c: blackbox.Context)(routesFile: c.Expr[String]): c.Expr[Routing] = {
    import c.universe._
    val liftables = new RouteLiftables {
      val universe: c.universe.type = c.universe
    }
    import liftables._

    val fileName = c.eval(c.Expr[String](c.untypecheck(routesFile.tree.duplicate)))
    val file = new File(fileName)

    val parent = file.getParentFile

    def readFile(fileName: String): util.Try[String] = util.Try {
      var source: Source = null
      try {
        source = Source.fromFile(new File(parent, fileName))
        source.mkString
      } finally {
        if (source != null) source.close()
      }
    }

    def parse(prefix: String, fileName: String): Routing = {
      def loop(fileName: String): List[Routing] = {
        readFile(fileName) match {
          case util.Success(content) ⇒
            RoutesFileParser.parseContent(content, file).fold({ errors ⇒
              val lines = content.split('\n')
              val message = errors.map(errorMessage(lines, _)).mkString("\n")
              c.abort(c.enclosingPosition, message)
            }, { routes ⇒
              routes.foldLeft[List[Routing]](Nil) {
                case (acc, route: Route) ⇒
                  acc :+ RouteDef(route)
                case (acc, include @ Include(prefix, router)) ⇒
                  val reference = router.replace(".Routes", ".routes")
                  acc :+ parse(prefix, reference)
              }
            })
          case util.Failure(error) ⇒
            c.warning(c.enclosingPosition, s"Skipping file $fileName")
            Nil
        }
      }
      RouteFile(prefix, fileName, loop(fileName))
    }

    def errorMessage(lines: Seq[String], error: RoutesCompilationError) = {
      val lineNumber = error.line.fold("")(":" + _ + error.column.fold("")(":" + _))
      val errorLine = error.line.flatMap { line ⇒
        val caret = error.column.map(c ⇒ (" " * (c - 1)) + "^").getOrElse("")
        lines.lift(line - 1).map(_ + "\n" + caret)
      }.getOrElse("")
      s"""|Error parsing routes file: ${error.source.getAbsolutePath}$lineNumber:\n
          |${error.message}
          |$errorLine
          |""".stripMargin
    }

    val result = parse("/", file.getName)
    c.Expr[Routing](q"$result")
  }

  def routesImpl(c: blackbox.Context)(routes: c.Expr[String]): c.Expr[Seq[Route]] = {
    import c.universe._
    val liftables = new RouteLiftables {
      val universe: c.universe.type = c.universe
    }
    import liftables._

    val content = c.eval(c.Expr[String](c.untypecheck(routes.tree.duplicate)))

    // These generic liftables avoid having to write Liftable instances by hand, or write custom quasiquotes for an ADT

    implicitly[Liftable[Route]]

    val file = writeRoutesFile(c)(content)

    val result = RoutesFileParser.parseContent(content, file).right.flatMap { routes ⇒
      routes.foldLeft[Either[Seq[RoutesCompilationError], Seq[Route]]](Right(Vector.empty[Route])) {
        case (Right(acc), route: Route) ⇒ Right(acc :+ route)
        case (l @ Left(_), _)           ⇒ l
        case (_, _)                     ⇒ Left(Seq(RoutesCompilationError(file, "doesn't support anything but route", None, None)))
      }
    }

    def errorMessage(lines: Seq[String], error: RoutesCompilationError) = {
      val lineNumber = error.line.fold("")(":" + _ + error.column.fold("")(":" + _))
      val errorLine = error.line.flatMap { line ⇒
        val caret = error.column.map(c ⇒ (" " * (c - 1)) + "^").getOrElse("")
        lines.lift(line - 1).map(_ + "\n" + caret)
      }.getOrElse("")
      s"""|Error parsing routes file: ${error.source.getAbsolutePath}$lineNumber:\n
          |${error.message}
          |$errorLine
          |""".stripMargin
    }

    result match {
      case Left(errors) ⇒
        val lines = content.split('\n')
        val message = errors.map(errorMessage(lines, _)).mkString("\n")
        c.abort(c.enclosingPosition, message)
      case Right(results) ⇒ c.Expr[Seq[Route]](q"$results")
    }
  }

  //TODO: switch to macro-compat
  def writeRoutesFile(c: blackbox.Context)(content: String): File = {
    val file = File.createTempFile("routes-compiler-", ".routes")
    // TODO: switch to info, so that -verbose is required to see it? (only for debugging)
    c.echo(c.universe.NoPosition, s"Writing routes file to ${file.getCanonicalPath}")
    file.deleteOnExit()
    val out = new PrintWriter(new BufferedWriter(new FileWriter(file)))
    try {
      out.println(content)
    } finally {
      out.close()
    }
    file
  }

}

