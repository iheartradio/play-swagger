package com.iheart.playSwagger.generator

import scala.util.matching.Regex

sealed abstract class NamingConvention(f: String => String) extends (String => String) {
  override def apply(keyName: String): String = f(keyName)
}

object NamingConvention {
  private val regex: Regex = "[A-Z\\d]".r
  private val skipNumberRegex: Regex = "[A-Z]".r

  object None extends NamingConvention(identity)
  object SnakeCase extends NamingConvention(x => regex.replaceAllIn(x, { m => "_" + m.group(0).toLowerCase() }))
  object KebabCase extends NamingConvention(x => regex.replaceAllIn(x, { m => "-" + m.group(0).toLowerCase() }))
  object LowerCase extends NamingConvention(x => regex.replaceAllIn(x, { m => m.group(0).toLowerCase() }))
  object UpperCamelCase extends NamingConvention(x => {
        val (head, tail) = x.splitAt(1)
        head.toUpperCase() + tail
      })
  object SnakeCaseSkipNumber extends NamingConvention(x =>
        skipNumberRegex.replaceAllIn(x, { m => "_" + m.group(0).toLowerCase() })
      )

  def fromString(naming: String): NamingConvention = naming match {
    case "snake_case" => SnakeCase
    case "snake_case_skip_number" => SnakeCaseSkipNumber
    case "kebab-case" => KebabCase
    case "lowercase" => LowerCase
    case "UpperCamelCase" => UpperCamelCase
    case _ => None
  }
}
