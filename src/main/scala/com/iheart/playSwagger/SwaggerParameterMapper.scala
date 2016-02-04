package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.SwaggerParameter
import play.api.libs.json._
import play.routes.compiler.Parameter

object SwaggerParameterMapper {

  def mapParam(parameter: Parameter, modelQualifier: DomainModelQualifier = DomainModelQualifier()): SwaggerParameter = {

    def higherOrderType(higherOrder: String, typeName: String): Option[String] = s"$higherOrder\\[(\\S+)\\]".r.findFirstMatchIn(typeName).map(_.group(1))

    def collectionItemType(typeName: String): Option[String] =
      List("Seq", "List", "Set", "Vector").map(higherOrderType(_, typeName)).reduce(_ orElse _)

    def swaggerParam(tp: String, format: Option[String] = None, required: Boolean = true) =
      SwaggerParameter(parameter.name, `type` = Some(tp), format = format, required = required)

    val typeName = parameter.typeName.replaceAll("(scala.)|(java.lang.)|(math.)|(org.joda.time.)", "")

    val defaultValueO: Option[JsValue] = {
      parameter.default.map { value ⇒
        typeName match {
          case ci"Int" | ci"Long"                      ⇒ JsNumber(value.toLong)
          case ci"Double" | ci"Float" | ci"BigDecimal" ⇒ JsNumber(value.toDouble)
          case ci"Boolean"                             ⇒ JsBoolean(value.toBoolean)
          case _                                       ⇒ JsString(value)
        }
      }
    }

    def isReference(tpeName: String = typeName): Boolean = modelQualifier.isModel(tpeName)
    lazy val optionalTypeO = higherOrderType("Option", typeName)
    lazy val itemTypeO = collectionItemType(typeName)

    def referenceParam(referenceType: String) =
      SwaggerParameter(parameter.name, referenceType = Some(referenceType))

    def optionalParam(optionalTpe: String) = {
      val param = if (isReference(optionalTpe)) referenceParam(optionalTpe) else mapParam(parameter.copy(typeName = optionalTpe))
      param.copy(required = false, default = defaultValueO)
    }

    def generalParam =
      (typeName match {
        case ci"Int"                     ⇒ swaggerParam("integer", Some("int32"))
        case ci"Long"                    ⇒ swaggerParam("integer", Some("int64"))
        case ci"Double" | ci"BigDecimal" ⇒ swaggerParam("number", Some("double"))
        case ci"Float"                   ⇒ swaggerParam("number", Some("float"))
        case ci"DateTime"                ⇒ swaggerParam("integer", Some("epoch"))
        case ci"Any"                     ⇒ swaggerParam("any").copy(example = Some(JsString("any JSON value")))
        case unknown                     ⇒ swaggerParam(unknown.toLowerCase())
      }).copy(
        default = defaultValueO,
        required = defaultValueO.isEmpty
      )

    if (isReference()) referenceParam(typeName)
    else if (optionalTypeO.isDefined)
      optionalParam(optionalTypeO.get)
    else if (itemTypeO.isDefined)
      SwaggerParameter(parameter.name, items = itemTypeO)
    else generalParam
  }

  implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci = ("(?i)" + sc.parts.mkString).r
  }

}
