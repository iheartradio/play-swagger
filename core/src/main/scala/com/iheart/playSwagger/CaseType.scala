package com.iheart.playSwagger

sealed trait CaseType {
  def transform(str: String): String
}

case object CamelCase extends CaseType {
  override def transform(str: String) = {
    (str.split("_").toList match {
      case head :: tail ⇒ head :: tail.map(_.capitalize)
      case x            ⇒ x
    }).mkString
  }
}

case object SnakeCase extends CaseType {
  override def transform(str: String) = {
    str.foldLeft(new StringBuilder) {
      case (s, c) if Character.isUpperCase(c) ⇒
        s.append("_").append(Character.toLowerCase(c))
      case (s, c) ⇒
        s.append(c)
    }.toString
  }
}