package com.iheart.playSwagger

import play.api.libs.json.JsValue

object Domain {
  type Path = String
  type Method = String

  final case class Definition(
    name:        String,
    properties:  Seq[SwaggerParameter],
    description: Option[String]        = None
  )

  final case class SwaggerParameter(
    name:          String,
    `type`:        Option[String]  = None,
    format:        Option[String]  = None,
    required:      Boolean         = true,
    default:       Option[JsValue] = None,
    example:       Option[JsValue] = None,
    referenceType: Option[String]  = None,
    items:         Option[String]  = None

  )
}

