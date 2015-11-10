package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.SwaggerParameter
import org.joda.time.DateTime
import play.api.libs.json.{ JsBoolean, JsNumber, JsValue, JsString }

object SwaggerParameterMapper {
  def mapParam(name: String, typeAndOrDefaultValue: String, domainNameSpace: Option[String] = None): SwaggerParameter = {

    def higherOrderType(higherOrder: String, typeName: String): Option[String] = s"$higherOrder\\[(\\S+)\\]".r.findFirstMatchIn(typeName).map(_.group(1))

    def collectionItemType(typeName: String): Option[String] =
      List("Seq", "List", "Set", "Vector").map(higherOrderType(_, typeName)).reduce(_ orElse _)

    def prop(tp: String, format: Option[String] = None, required: Boolean = true) =
      SwaggerParameter(name, `type` = Some(tp), format = format, required = required)

    val parts = typeAndOrDefaultValue.split("\\?=")

    val typePart = parts.head.stripMargin
    val typeName = typePart.replace("scala.", "").replace("java.lang.", "")

    val defaultValue: Option[JsValue] = {

      if (parts.length == 2) {
        val stringVal = parts.last.stripMargin
        Some(typeName match {
          case "Int" | "Long" ⇒ JsNumber(stringVal.toLong)
          case "Double" | "Float" ⇒ JsNumber(stringVal.toDouble)
          case "Boolean" ⇒ JsBoolean(stringVal.toBoolean)
          case _ ⇒ JsString(stringVal)
        })
      } else None
    }

    def isReference(tpeName: String): Boolean = domainNameSpace.fold(false)(tpeName.startsWith(_))

    if (isReference(typeName))
      SwaggerParameter(name, referenceType = Some(typeName))
    else {
      val optionalType = higherOrderType("Option", typeName)
      val itemType = collectionItemType(typeName)
      if (itemType.isDefined)
        SwaggerParameter(name, items = itemType)
      else if (optionalType.isDefined)
        (if (isReference(optionalType.get))
          SwaggerParameter(name, referenceType = optionalType)
        else
          mapParam(name, optionalType.get)).copy(required = false, default = defaultValue)
      else
        (typeName match {
          case "Int" ⇒ prop("integer", Some("int32"))
          case "Long" ⇒ prop("integer", Some("int64"))
          case "Double" ⇒ prop("number", Some("double"))
          case "Float" ⇒ prop("number", Some("float"))
          case "org.jodaTime.DateTime" ⇒ prop("integer", Some("epoch"))
          case "Any" ⇒ prop("any").copy(example = Some(JsString("any JSON value")))
          case unknown ⇒ prop(unknown.toLowerCase())
        }).copy(default = defaultValue, required = defaultValue.isEmpty)
    }

  }
}
