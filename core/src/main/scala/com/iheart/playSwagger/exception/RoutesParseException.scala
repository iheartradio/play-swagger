package com.iheart.playSwagger.exception

import com.iheart.playSwagger.exception.RoutesParseException.RoutesParseErrorDetail

class RoutesParseException(errors: Seq[RoutesParseErrorDetail]) extends RuntimeException(
      errors.map { error =>
        val caret = error.column.map(c => (" " * (c - 1)) + "^").getOrElse("")
        // line Number がある場合は ":" と共に表記する
        val lineNumberText = error.lineNumber.fold("")(n => f":$n")
        s"""|Error parsing routes file: ${error.sourceFileName}$lineNumberText ${error.message}
        |${error.content.fold("")(_)}
        |$caret
        |""".stripMargin
      }.mkString("\n")
    )

object RoutesParseException {
  case class RoutesParseErrorDetail(
      sourceFileName: String,
      message: String,
      content: Option[String] = None,
      lineNumber: Option[Int] = None,
      column: Option[Int] = None
  )
}
