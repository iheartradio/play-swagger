package com.iheart.playSwagger

import scala.util.matching.Regex

sealed abstract class NamingStrategy(f: String ⇒ String) extends (String ⇒ String) {
  override def apply(keyName: String): String = f(keyName)
}

object NamingStrategy {
  val regex: Regex = "[A-Z\\d]".r

  object None extends NamingStrategy(identity)
  object SnakeCase extends NamingStrategy(x ⇒ regex.replaceAllIn(x, { m ⇒ "_" + m.group(0).toLowerCase() }))
  object KebabCase extends NamingStrategy(x ⇒ regex.replaceAllIn(x, { m ⇒ "-" + m.group(0).toLowerCase() }))
  object LowerCase extends NamingStrategy(x ⇒ regex.replaceAllIn(x, { m ⇒ m.group(0).toLowerCase() }))
  object UpperCamelCase extends NamingStrategy(x ⇒ {
    val (head, tail) = x.splitAt(1)
    head.toUpperCase() + tail
  })

  def from(naming: String): NamingStrategy = naming match {
    case "snake_case"     ⇒ SnakeCase
    case "kebab-case"     ⇒ KebabCase
    case "lowercase"      ⇒ LowerCase
    case "UpperCamelCase" ⇒ UpperCamelCase
    case _                ⇒ None
  }
}
