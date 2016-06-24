package com.iheart.playSwagger

trait DomainModelQualifier {
  def isModel(className: String): Boolean
}

final case class PrefixDomainModelQualifier(namespaces: String*) extends DomainModelQualifier {
  def isModel(className: String): Boolean = namespaces exists className.startsWith
}
