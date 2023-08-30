package com.iheart.playSwagger.generator

trait DomainModelQualifier {

  /** あるクラスがドメインモデルとして定義されているかを確認する */
  def isModel(className: String): Boolean
}

/** パッケージ名のリストを用いてドメインモデルかどうかを判別する */
final case class PrefixDomainModelQualifier(namespaces: String*) extends DomainModelQualifier {
  def isModel(className: String): Boolean = namespaces exists className.startsWith
}
