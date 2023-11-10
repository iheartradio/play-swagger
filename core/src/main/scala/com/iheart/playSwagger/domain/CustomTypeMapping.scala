package com.iheart.playSwagger.domain

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsObject, JsPath, Reads}

case class CustomTypeMapping(
    `type`: String,
    specAsParameter: List[JsObject] = Nil,
    specAsProperty: Option[JsObject] = None,
    required: Boolean = true
)

object CustomTypeMapping {
  implicit val reads: Reads[CustomTypeMapping] = (
    (JsPath \ "type").read[String] and
      (JsPath \ "specAsParameter").read[List[JsObject]] and
      (JsPath \ "specAsProperty").readNullable[JsObject] and
      (JsPath \ "required").read[Boolean].orElse(Reads.pure(true))
  )(CustomTypeMapping.apply _)
}
