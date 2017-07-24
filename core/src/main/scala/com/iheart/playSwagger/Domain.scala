package com.iheart.playSwagger

import play.api.libs.json.{JsObject, JsValue}
import play.twirl.api.TemplateMagic.Default

object Domain {
  type Path = String
  type Method = String

  final case class Definition(
    name:        String,
    properties:  Seq[SwaggerParameter],
    description: Option[String]        = None
  )

  sealed trait SwaggerParameter {
    def name: String
    def required: Boolean
    def default: Option[JsValue]

    def update(required: Boolean, default: Option[JsValue]): SwaggerParameter
  }

  final case class GenSwaggerParameter(
    name:          String,
    `type`:        Option[String]           = None,
    format:        Option[String]           = None,
    required:      Boolean                  = true,
    default:       Option[JsValue]          = None,
    example:       Option[JsValue]          = None,
    referenceType: Option[String]           = None,
    items:         Option[SwaggerParameter] = None,
    enum:          Option[Seq[String]]      = None
  ) extends SwaggerParameter {
    def update(_required: Boolean, _default: Option[JsValue]) =
      copy(required = _required, default = _default)
  }

  final case class CustomSwaggerParameter(
    name:            String,
    specAsParameter: List[JsObject],
    specAsProperty:  Option[JsObject],
    required:        Boolean          = true,
    default:         Option[JsValue]  = None
  ) extends SwaggerParameter {
    def update(_required: Boolean, _default: Option[JsValue]) =
      copy(required = _required, default = _default)
  }

  type CustomMappings = List[CustomTypeMapping]

  case class CustomTypeMapping(
    `type`:          String,
    specAsParameter: List[JsObject]   = Nil,
    specAsProperty:  Option[JsObject] = None,
    required:        Boolean          = true
  )
}

