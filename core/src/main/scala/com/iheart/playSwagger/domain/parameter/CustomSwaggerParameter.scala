package com.iheart.playSwagger.domain.parameter

import play.api.libs.json.{JsObject, JsValue}

final case class CustomSwaggerParameter(
    override val name: String,
    specAsParameter: List[JsObject],
    specAsProperty: Option[JsObject],
    override val required: Boolean = true,
    override val nullable: Option[Boolean] = None,
    override val default: Option[JsValue] = None,
    override val description: Option[String] = None
) extends SwaggerParameter {
  def update(_required: Boolean, _nullable: Boolean, _default: Option[JsValue]): CustomSwaggerParameter =
    copy(required = _required, nullable = Some(_nullable), default = _default)
}
