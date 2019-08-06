package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.{ CustomMappings, CustomSwaggerParameter, GenSwaggerParameter, SwaggerParameter }
import play.api.libs.json._
import play.routes.compiler.Parameter
import scala.reflect.runtime.universe

import scala.util.Try

object SwaggerParameterMapper {

  type MappingFunction = PartialFunction[String, SwaggerParameter]

  def mapParam(
    parameter:      Parameter,
    modelQualifier: DomainModelQualifier = PrefixDomainModelQualifier(),
    customMappings: CustomMappings       = Nil)(implicit cl: ClassLoader): SwaggerParameter = {

    def removeKnownPrefixes(name: String) = name.replaceAll("^((scala\\.)|(java\\.lang\\.)|(math\\.)|(org\\.joda\\.time\\.))", "")

    def higherOrderType(higherOrder: String, typeName: String): Option[String] = {
      s"$higherOrder\\[(\\S+)\\]".r.findFirstMatchIn(typeName).map(_.group(1))
    }

    def collectionItemType(typeName: String): Option[String] =
      List("Seq", "List", "Set", "Vector").map(higherOrderType(_, typeName)).reduce(_ orElse _)

    val typeName = removeKnownPrefixes(parameter.typeName)

    val defaultValueO: Option[JsValue] = {
      parameter.default.map { value ⇒
        typeName match {
          case ci"Int" | ci"Long"                      ⇒ JsNumber(value.toLong)
          case ci"Double" | ci"Float" | ci"BigDecimal" ⇒ JsNumber(value.toDouble)
          case ci"Boolean"                             ⇒ JsBoolean(value.toBoolean)
          case ci"String" ⇒ {
            val noquotes = value match {
              case c if c.startsWith("\"\"\"") && c.endsWith("\"\"\"") ⇒ c.substring(3, c.length - 3)
              case c if c.startsWith("\"") && c.endsWith("\"") ⇒ c.substring(1, c.length - 1)
              case c ⇒ c
            }
            JsString(noquotes)
          }
          case _ ⇒ JsString(value)
        }
      }
    }

    def genSwaggerParameter(
      tp:     String,
      format: Option[String]      = None,
      enum:   Option[Seq[String]] = None) =
      GenSwaggerParameter(
        parameter.name,
        `type` = Some(tp),
        format = format,
        required = defaultValueO.isEmpty,
        default = defaultValueO,
        enum = enum)

    val enumParamMF: MappingFunction = {
      case JavaEnum(enumConstants)  ⇒ genSwaggerParameter("string", enum = Option(enumConstants))
      case ScalaEnum(enumConstants) ⇒ genSwaggerParameter("string", enum = Option(enumConstants))
    }

    def isReference(tpeName: String = typeName): Boolean = modelQualifier.isModel(tpeName)

    def referenceParam(referenceType: String) =
      GenSwaggerParameter(parameter.name, referenceType = Some(referenceType))

    def optionalParam(optionalTpe: String) = {
      val asRequired = mapParam(parameter.copy(typeName = optionalTpe), modelQualifier = modelQualifier, customMappings = customMappings)
      asRequired.update(required = false, default = asRequired.default)
    }

    def updateGenParam(param: SwaggerParameter)(update: GenSwaggerParameter ⇒ GenSwaggerParameter): SwaggerParameter = param match {
      case p: GenSwaggerParameter ⇒ update(p)
      case _                      ⇒ param
    }

    val referenceParamMF: MappingFunction = {
      case tpe if isReference(tpe) ⇒ referenceParam(tpe)
    }

    val optionalParamMF: MappingFunction = {
      case tpe if higherOrderType("Option", typeName).isDefined ⇒
        optionalParam(higherOrderType("Option", typeName).get)
    }

    val generalParamMF: MappingFunction = {
      case ci"Int"                     ⇒ genSwaggerParameter("integer", Some("int32"))
      case ci"Long"                    ⇒ genSwaggerParameter("integer", Some("int64"))
      case ci"Double" | ci"BigDecimal" ⇒ genSwaggerParameter("number", Some("double"))
      case ci"Float"                   ⇒ genSwaggerParameter("number", Some("float"))
      case ci"DateTime"                ⇒ genSwaggerParameter("integer", Some("epoch"))
      case ci"Any"                     ⇒ genSwaggerParameter("any").copy(example = Some(JsString("any JSON value")))
      case unknown                     ⇒ genSwaggerParameter(unknown.toLowerCase())
    }

    val itemsParamMF: MappingFunction = {
      case tpe if collectionItemType(tpe).isDefined ⇒
        // TODO: This could use a different type to represent ItemsObject(http://swagger.io/specification/#itemsObject),
        // since the structure is not quite the same, and still has to be handled specially in a json transform (see propWrites in SwaggerSpecGenerator)
        // However, that spec conflicts with example code elsewhere that shows other fields in the object, such as properties:
        // http://stackoverflow.com/questions/26206685/how-can-i-describe-complex-json-model-in-swagger
        updateGenParam(generalParamMF("array"))(_.copy(
          items = Some(
            mapParam(parameter.copy(typeName = collectionItemType(tpe).get), modelQualifier, customMappings))))
    }

    val customMappingMF: MappingFunction = customMappings.map { mapping ⇒
      val re = StringContext(removeKnownPrefixes(mapping.`type`)).ci
      val mf: MappingFunction = {
        case re() ⇒
          CustomSwaggerParameter(
            parameter.name,
            mapping.specAsParameter,
            mapping.specAsProperty,
            default = defaultValueO,
            required = defaultValueO.isEmpty && mapping.required)
      }
      mf
    }.foldLeft[MappingFunction](PartialFunction.empty)(_ orElse _)

    // sequence of this list is the sequence of matching, that is, of importance
    List(
      optionalParamMF,
      itemsParamMF,
      customMappingMF,
      enumParamMF,
      referenceParamMF,
      generalParamMF).reduce(_ orElse _)(typeName)

  }

  implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci = ("(?i)" + sc.parts.mkString).r
  }

  /**
   * Unapply the type by name and return the Java enum constants if those exist.
   */
  private object JavaEnum {
    def unapply(tpeName: String)(implicit cl: ClassLoader): Option[Seq[String]] = {
      Try(cl.loadClass(tpeName)).toOption.filter(_.isEnum).map(_.getEnumConstants.map(_.toString))
    }
  }

  /**
   * Unapply the type by name and return the Scala enum constants if those exist.
   */
  private object ScalaEnum {
    def unapply(tpeName: String)(implicit cl: ClassLoader): Option[Seq[String]] = {
      if (tpeName.endsWith(".Value")) {
        Try {
          val mirror = universe.runtimeMirror(cl)
          val module = mirror.reflectModule(mirror.staticModule(tpeName.stripSuffix(".Value")))
          for {
            enum ← Option(module.instance).toSeq if enum.isInstanceOf[Enumeration]
            value ← enum.asInstanceOf[Enumeration].values.asInstanceOf[Iterable[Enumeration#Value]]
          } yield value.toString
        }.toOption.filterNot(_.isEmpty)
      } else
        None
    }
  }

}

