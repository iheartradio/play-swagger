package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.SwaggerParameter
import play.api.libs.json._
import play.routes.compiler.Parameter

import scala.util.Try

object SwaggerParameterMapper {

  def mapParam(parameter: Parameter, modelQualifier: DomainModelQualifier = PrefixDomainModelQualifier())(implicit cl: ClassLoader): SwaggerParameter = {

    def higherOrderType(higherOrder: String, typeName: String): Option[String] = {
      s"$higherOrder\\[(\\S+)\\]".r.findFirstMatchIn(typeName).map(_.group(1))
    }

    def collectionItemType(typeName: String): Option[String] =
      List("Seq", "List", "Set", "Vector").map(higherOrderType(_, typeName)).reduce(_ orElse _)

    def swaggerParam(tp: String, format: Option[String] = None, required: Boolean = true, enum: Option[Seq[String]] = None) =
      SwaggerParameter(parameter.name, `type` = Some(tp), format = format, required = required, enum = enum)

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

    def getJavaEnum(tpeName: String): Option[Class[java.lang.Enum[_]]] = {
      Try(cl.loadClass(tpeName)).toOption.filter(_.isEnum).map(_.asInstanceOf[Class[java.lang.Enum[_]]])
    }

    def enumParam(tpeName: String) = {
      val enumConstants = getJavaEnum(tpeName).get.getEnumConstants.map(_.toString).toSeq
      swaggerParam("string", enum = Option(enumConstants))
    }

    def isReference(tpeName: String = typeName): Boolean = modelQualifier.isModel(tpeName)

    lazy val optionalTypeO = higherOrderType("Option", typeName)

    def referenceParam(referenceType: String) =
      SwaggerParameter(parameter.name, referenceType = Some(referenceType))

    def optionalParam(optionalTpe: String) = {
      val param = if (isReference(optionalTpe)) referenceParam(optionalTpe) else mapParam(parameter.copy(typeName = optionalTpe), modelQualifier = modelQualifier)
      param.copy(required = false, default = defaultValueO)
    }

    def generalParam(tpe: String = typeName) =
      (tpe match {
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

    lazy val itemTypeO = collectionItemType(typeName)

    if (getJavaEnum(typeName).isDefined) enumParam(typeName)
    else if (isReference()) referenceParam(typeName)
    else if (optionalTypeO.isDefined)
      optionalParam(optionalTypeO.get)
    else if (itemTypeO.isDefined)
      // TODO: This could use a different type to represent ItemsObject(http://swagger.io/specification/#itemsObject),
      // since the structure is not quite the same, and still has to be handled specially in a json transform (see propFormat in SwaggerSpecGenerator)
      // However, that spec conflicts with example code elsewhere that shows other fields in the object, such as properties:
      // http://stackoverflow.com/questions/26206685/how-can-i-describe-complex-json-model-in-swagger
      generalParam("array").copy(
        items = Some(
          mapParam(parameter.copy(typeName = itemTypeO.get), modelQualifier)
        )
      )
    else generalParam()
  }

  implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci = ("(?i)" + sc.parts.mkString).r
  }

}
