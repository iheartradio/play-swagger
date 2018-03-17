package com.iheart.playSwagger

import play.api.libs.json.{ JsObject, JsPath, JsValue, Reads }
import play.twirl.api.TemplateMagic.Default

object Domain {
  type Path = String
  type Method = String

  final case class Definition(
    name:        String,
    properties:  Seq[SwaggerParameter],
    description: Option[String]        = None)

  sealed trait SwaggerParameter {
    def name: String
    def required: Boolean
    def default: Option[JsValue]
    def description: Option[String]

    def update(required: Boolean, default: Option[JsValue]): SwaggerParameter
  }

  final case class GenSwaggerParameter(
    name:          String,
    referenceType: Option[String]           = None,
    `type`:        Option[String]           = None,
    format:        Option[String]           = None,
    required:      Boolean                  = true,
    default:       Option[JsValue]          = None,
    example:       Option[JsValue]          = None,
    items:         Option[SwaggerParameter] = None,
    enum:          Option[Seq[String]]      = None,
    description:   Option[String]           = None) extends SwaggerParameter {
    def update(_required: Boolean, _default: Option[JsValue]) =
      copy(required = _required, default = _default)
  }

  final case class CustomSwaggerParameter(
    name:            String,
    specAsParameter: List[JsObject],
    specAsProperty:  Option[JsObject],
    required:        Boolean          = true,
    default:         Option[JsValue]  = None,
    description:     Option[String]   = None) extends SwaggerParameter {
    def update(_required: Boolean, _default: Option[JsValue]) =
      copy(required = _required, default = _default)
  }

  type CustomMappings = List[CustomTypeMapping]

  case class CustomTypeMapping(
    `type`:          String,
    specAsParameter: List[JsObject]   = Nil,
    specAsProperty:  Option[JsObject] = None,
    required:        Boolean          = true)

  object CustomTypeMapping {
    import play.api.libs.functional.syntax._
    implicit val csmFormat: Reads[CustomTypeMapping] = (
      (JsPath \ 'type).read[String] and
      (JsPath \ 'specAsParameter).read[List[JsObject]] and
      (JsPath \ 'specAsProperty).readNullable[JsObject] and
      ((JsPath \ 'required).read[Boolean] orElse Reads.pure(true)))(CustomTypeMapping.apply _)
  }
}

