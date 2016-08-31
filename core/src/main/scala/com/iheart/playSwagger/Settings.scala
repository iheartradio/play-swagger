package com.iheart.playSwagger

import play.api.libs.json.Json

case class SwaggerMapping(fromType: String, toType: String, format: Option[String] = None)

object SwaggerMapping {
  implicit val format = Json.format[SwaggerMapping]
}

case class Settings(mappings: Seq[SwaggerMapping] = Seq())

object Settings {
  implicit val format = Json.format[Settings]
}

