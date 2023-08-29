package com.iheart.playSwagger

import com.iheart.playSwagger.domain.parameter.SwaggerParameter

object Domain {
  type Path = String
  type Method = String

  final case class Definition(
      name: String,
      properties: Seq[SwaggerParameter],
      description: Option[String] = None
  )
}
