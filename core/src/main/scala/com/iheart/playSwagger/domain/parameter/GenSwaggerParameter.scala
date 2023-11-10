package com.iheart.playSwagger.domain.parameter

import play.api.libs.json.JsValue

final case class GenSwaggerParameter private (
    override val name: String,
    override val required: Boolean,
    override val description: Option[String] = None,
    referenceType: Option[String] = None,
    `type`: Option[String] = None,
    format: Option[String] = None,
    override val nullable: Option[Boolean] = None,
    override val default: Option[JsValue] = None,
    example: Option[JsValue] = None,
    items: Option[SwaggerParameter] = None,
    enum: Option[Seq[String]] = None
) extends SwaggerParameter {
  override def update(_required: Boolean, _nullable: Boolean, _default: Option[JsValue]): GenSwaggerParameter =
    copy(required = _required, nullable = Some(_nullable), default = _default)
}

object GenSwaggerParameter {
  def apply(
      `type`: String,
      format: Option[String],
      enum: Option[Seq[String]]
  )(implicit name: String, default: Option[JsValue], description: Option[String]): GenSwaggerParameter =
    new GenSwaggerParameter(
      name = name,
      `type` = Some(`type`),
      format = format,
      required = default.isEmpty,
      default = default,
      enum = enum,
      description = description
    )

  def apply(name: String, `type`: String): GenSwaggerParameter =
    new GenSwaggerParameter(name = name, required = true, `type` = Some(`type`))
}
