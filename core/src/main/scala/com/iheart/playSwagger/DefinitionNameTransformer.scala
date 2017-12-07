package com.iheart.playSwagger

trait DefinitionNameTransformer {
  def transform(str: String): String
}

final class NoTransformer extends DefinitionNameTransformer {
  override def transform(str: String) = str
}

final class CamelcaseTransformer extends DefinitionNameTransformer {
  override def transform(str: String) = {
    (str.split("_").toList match {
      case head :: tail ⇒ head :: tail.map(_.capitalize)
      case x            ⇒ x
    }).mkString
  }
}

final class SnakecaseTransformer extends DefinitionNameTransformer {
  override def transform(str: String) = {
    str.foldLeft(new StringBuilder) {
      case (s, c) if Character.isUpperCase(c) ⇒
        s.append("_").append(Character.toLowerCase(c))
      case (s, c) ⇒
        s.append(c)
    }.toString
  }
}