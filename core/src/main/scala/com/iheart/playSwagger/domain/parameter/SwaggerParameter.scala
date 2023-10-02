package com.iheart.playSwagger.domain.parameter

import play.api.libs.json.JsValue

/** [[https://swagger.io/specification/?sbsearch=-schema%20-object#parameter-object Parameter Object]] */
trait SwaggerParameter {
  def name: String

  def required: Boolean

  def nullable: Option[Boolean]

  def default: Option[JsValue]

  def description: Option[String]

  def update(required: Boolean, nullable: Boolean, default: Option[JsValue]): SwaggerParameter
}
