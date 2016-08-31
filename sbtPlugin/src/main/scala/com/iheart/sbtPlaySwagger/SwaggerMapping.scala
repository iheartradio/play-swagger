package com.iheart.sbtPlaySwagger

case class SwaggerMapping(fromType: String, toType: String, format: Option[String] = None) {
  def toJson: String = {
    val formatStr = format.map(x ⇒ s""", "format": "$x"""").getOrElse("")

    s"""{ "fromType": "$fromType", "toType": "$toType"$formatStr }"""
  }
}
