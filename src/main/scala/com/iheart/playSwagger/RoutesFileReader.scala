package com.iheart.playSwagger

import ResourceReader.read

import scala.util.{Failure, Success, Try}

final case class RoutesFileReader(implicit cl: ClassLoader) {
  type ResourceName = String

  def readAll(rootName: String = RoutesFileReader.rootRoute): Try[Map[APIName, List[Line]]] =
    readRoutes(rootName)

  private def imports(routeFileContent: List[Line]): List[ResourceName] = {
    routeFileContent.
      map(_.stripMargin).
      filter(_.startsWith("->")).
      map(_.split("\\s").last.replace(".Routes", ".routes"))
  }

  private def readRoutes(routeFileName: ResourceName): Try[Map[APIName, List[Line]]] = {
    read(routeFileName).flatMap { lines ⇒
      val thisEntry = Map(routeFileName.replace(".routes", "") → lines)
      val init: Try[Map[APIName, List[Line]]] = Success(thisEntry)
      imports(lines).foldLeft(Success(thisEntry): Try[Map[APIName, List[Line]]]) {
        case (Success(memo), importFile) ⇒ readRoutes(importFile).map(memo ++ _)
        case (f @ Failure(_), _)         ⇒ f
      }
    }

  }
}

object RoutesFileReader {
  val rootRoute = "routes"
}
