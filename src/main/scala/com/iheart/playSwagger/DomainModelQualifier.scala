package com.iheart.playSwagger

case class DomainModelQualifier(namespaces: String*) {
  def isModel(className: String): Boolean = namespaces.exists(className.startsWith(_))
}
