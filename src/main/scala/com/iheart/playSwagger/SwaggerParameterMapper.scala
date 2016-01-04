package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.SwaggerParameter
import org.joda.time.DateTime
import play.api.libs.json._

object SwaggerParameterMapper {
  def mapParam(name: String, typeAndOrDefaultValue: String, modelQualifier: DomainModelQualifier = DomainModelQualifier()): SwaggerParameter = {

    def higherOrderType(higherOrder: String, typeName: String): Option[String] = s"$higherOrder\\[(\\S+)\\]".r.findFirstMatchIn(typeName).map(_.group(1))

    def collectionItemType(typeName: String): Option[String] =
      List("Seq", "List", "Set", "Vector").map(higherOrderType(_, typeName)).reduce(_ orElse _)

    def swaggerParam(tp: String, format: Option[String] = None, required: Boolean = true) =
      SwaggerParameter(name, `type` = Some(tp), format = format, required = required)

    val parts = typeAndOrDefaultValue.split("\\?=")

    val typePart = parts.head.stripMargin
    val typeName = typePart.replace("scala.", "").replace("java.lang.", "")

    val defaultValueO: Option[JsValue] = {
      if (parts.length == 2) {
        val stringVal = parts.last.stripMargin
        Some(typeName match {
          case "Int" | "Long"     ⇒ JsNumber(stringVal.toLong)
          case "Double" | "Float" ⇒ JsNumber(stringVal.toDouble)
          case "Boolean"          ⇒ JsBoolean(stringVal.toBoolean)
          case _                  ⇒ JsString(stringVal)
        })
      } else None
    }

    def isReference(tpeName: String = typeName): Boolean = modelQualifier.isModel(tpeName)
    lazy val optionalTypeO = higherOrderType("Option", typeName)
    lazy val itemTypeO = collectionItemType(typeName)

    def referenceParam(referenceType: String) =
      SwaggerParameter(name, referenceType = Some(referenceType))

    def optionalParam(optionalTpe: String) = {
      val param = if (isReference(optionalTpe)) referenceParam(optionalTpe) else mapParam(name, optionalTpe)
      param.copy(required = false, default = defaultValueO)
    }

    def generalParam =
      (typeName match {
        case "Int"                    ⇒ swaggerParam("integer", Some("int32"))
        case "Long"                   ⇒ swaggerParam("integer", Some("int64"))
        case "Double"                 ⇒ swaggerParam("number", Some("double"))
        case "Float"                  ⇒ swaggerParam("number", Some("float"))
        case "org.joda.time.DateTime" ⇒ swaggerParam("integer", Some("epoch"))
        case "Any"                    ⇒ swaggerParam("any").copy(example = Some(JsString("any JSON value")))
        case unknown                  ⇒ swaggerParam(unknown.toLowerCase())
      }).copy(default = defaultValueO, required = defaultValueO.isEmpty)

    if (isReference()) referenceParam(typeName)
    else if (optionalTypeO.isDefined)
      optionalParam(optionalTypeO.get)
    else if (itemTypeO.isDefined)
      SwaggerParameter(name, items = itemTypeO)
    else generalParam
  }
}
