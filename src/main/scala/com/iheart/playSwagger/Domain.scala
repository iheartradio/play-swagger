package com.iheart.playSwagger

import play.api.libs.json.JsValue

object Domain {
  type Path = String
  type Method = String

  case class Definition(
    name: String,
    properties: Seq[SwaggerParameter],
    description: Option[String] = None
  )

  case class SwaggerParameter(
    name: String,
    `type`: Option[String] = None,
    format: Option[String] = None,
    required: Boolean = true,
    example: Option[JsValue] = None,
    referenceType: Option[String] = None,
    items: Option[String] = None
  )
}

