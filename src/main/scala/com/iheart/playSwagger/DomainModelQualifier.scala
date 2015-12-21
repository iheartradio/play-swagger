package com.iheart.playSwagger

final case class DomainModelQualifier(namespaces: String*) {
  def isModel(className: String): Boolean = namespaces exists className.startsWith
}
