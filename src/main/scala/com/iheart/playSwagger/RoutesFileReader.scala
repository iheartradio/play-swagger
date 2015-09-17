package com.iheart.playSwagger

import ResourceReader.read
case class RoutesFileReader(implicit cl: ClassLoader) {
  type ResourceName = String

  def readAll(rootName: String = RoutesFileReader.rootRoute): Map[APIName, List[Line]] = readRoutes(rootName)

  private def imports(routeFileContent: List[Line]): List[ResourceName] = {
    routeFileContent.
      map(_.stripMargin).
      filter(_.startsWith("->")).
      map(_.split("\\s").last.replace(".Routes", ".routes"))
  }

  private def readRoutes(routeFileName: ResourceName): Map[APIName, List[Line]] = {
    val lines = read(routeFileName)
    val thisEntry = Map(routeFileName.replace(".routes", "") -> lines)

    imports(lines).foldLeft(thisEntry){ (memo, importFile) ⇒ memo ++ readRoutes(importFile) }
  }
}


object RoutesFileReader {
  val rootRoute = "routes"
}
