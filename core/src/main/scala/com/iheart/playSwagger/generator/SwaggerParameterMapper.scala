package com.iheart.playSwagger.generator

import scala.reflect.runtime.universe
import scala.util.Try
import scala.util.matching.Regex

import com.iheart.playSwagger.domain.CustomTypeMapping
import com.iheart.playSwagger.domain.parameter.{CustomSwaggerParameter, GenSwaggerParameter, SwaggerParameter}
import play.api.libs.json._
import play.routes.compiler.Parameter

class SwaggerParameterMapper(
    customMappings: Seq[CustomTypeMapping] = Nil,
    val modelQualifier: DomainModelQualifier
) {

  type MappingFunction = PartialFunction[String, SwaggerParameter]

  def mapParam(
      parameter: Parameter,
      description: Option[String]
  )(implicit cl: ClassLoader): SwaggerParameter = {
    val typeName = removeKnownPrefixes(parameter.typeName)
    mapParam(
      typeName,
      parameter.name,
      parameter.default.map(defaultValueO(_, typeName)),
      description
    )
  }

  private def mapParam(
      typeName: String,
      name: String,
      default: Option[JsValue],
      description: Option[String] = None
  )(implicit cl: ClassLoader): SwaggerParameter = {
    val tpe = removeKnownPrefixes(typeName)
    implicit val implicitName: String = name
    implicit val implicitDefault: Option[JsValue] = default
    implicit val implicitDescription: Option[String] = description
    // sequence of this list is the sequence of matching, that is, of importance
    List(
      optionalParamMF,
      itemsParamMF,
      customMappingMF,
      enumParamMF,
      referenceParamMF,
      generalParamMF
    ).reduce(_ orElse _)(tpe)
  }

  /* Mapper 内で直接参照されるパッケージのうち、標準で定義されているクラスのパッケージ名を削除 */
  private def removeKnownPrefixes(name: String): String =
    name.replaceAll("^((scala\\.)|(java\\.lang\\.)|(java\\.util\\.)|(math\\.)|(org\\.joda\\.time\\.))", "")

  /**
    * 単一型パラメータのジェネリクスが指定された場合に、型パラメータを取り出す
    *
    * @param higherOrder ジェネリッククラス
    * @param typeName    ジェネリクスの型情報
    * @param pkgPattern  ジェネリッククラスのパッケージのパターン
    * @return 型パラメータの名前
    */
  private def higherOrderType(higherOrder: String, typeName: String, pkgPattern: Option[String]): Option[String] = {
    (s"^${pkgPattern.map(p => s"(?:$p\\.)?").getOrElse("")}$higherOrder\\[(\\S+)\\]").r
      .findFirstMatchIn(typeName)
      .map(_.group(1))
  }

  /** typeName にコレクションが渡された際、要素の型を返却する */
  private def collectionItemType(typeName: String): Option[String] =
    List("Seq", "List", "Set", "Vector")
      .map(higherOrderType(_, typeName, Some("collection(?:\\.(?:mutable|immutable))?")))
      .reduce(_ orElse _)

  private def defaultValueO(default: String, typeName: String): JsValue = {
    if (default.equals("null")) {
      JsNull
    } else {
      typeName match {
        // Java の場合は int, Scala の場合は Int という命名になっているため、区別しない
        case ci"Int" | ci"Long" => JsNumber(default.toLong)
        case ci"Double" | ci"Float" | ci"BigDecimal" => JsNumber(default.toDouble)
        case ci"Boolean" => JsBoolean(default.toBoolean)
        case ci"String" =>
          // router では `func(value ?= "default value")` 形式で定義されるため、 `"` を削除する
          val unquotedString = default match {
            case c if c.startsWith("\"\"\"") && c.endsWith("\"\"\"") => c.substring(3, c.length - 3)
            case c if c.startsWith("\"") && c.endsWith("\"") => c.substring(1, c.length - 1)
            case c => c
          }
          JsString(unquotedString)
        case _ => JsString(default)
      }
    }
  }

  private def generalParamMF(
      implicit name: String,
      default: Option[JsValue],
      description: Option[String]
  ): MappingFunction = {
    case ci"Int" | ci"Integer" => GenSwaggerParameter("integer", Some("int32"), None)
    case ci"Long" => GenSwaggerParameter("integer", Some("int64"), None)
    case ci"Double" | ci"BigDecimal" => GenSwaggerParameter("number", Some("double"), None)
    case ci"Float" => GenSwaggerParameter("number", Some("float"), None)
    case ci"DateTime" => GenSwaggerParameter("integer", Some("epoch"), None)
    case ci"java.time.Instant" => GenSwaggerParameter("string", Some("date-time"), None)
    case ci"java.time.LocalDate" => GenSwaggerParameter("string", Some("date"), None)
    case ci"java.time.LocalDateTime" => GenSwaggerParameter("string", Some("date-time"), None)
    case ci"java.time.Duration" => GenSwaggerParameter(`type` = "string", None, None)
    case ci"Any" => GenSwaggerParameter(`type` = "any", None, None).copy(example = Some(JsString("any JSON value")))
    case unknown => GenSwaggerParameter(`type` = unknown.toLowerCase(), None, None)
  }

  private def enumParamMF(
      implicit name: String,
      default: Option[JsValue],
      description: Option[String],
      cl: ClassLoader
  ): MappingFunction = {
    case JavaEnum(enumConstants) => GenSwaggerParameter(`type` = "string", format = None, enum = Option(enumConstants))
    case ScalaEnum(enumConstants) => GenSwaggerParameter(`type` = "string", format = None, enum = Option(enumConstants))
    case EnumeratumEnum(enumConstants) =>
      GenSwaggerParameter(`type` = "string", format = None, enum = Option(enumConstants))
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
    * see: [[https://github.com/iheartradio/play-swagger/pull/125]]
    */
  private object ScalaEnum {
    def unapply(tpeName: String)(implicit cl: ClassLoader): Option[Seq[String]] = {
      if (tpeName.endsWith(".Value")) {
        Try {
          val mirror = universe.runtimeMirror(cl)
          val module = mirror.reflectModule(mirror.staticModule(tpeName.stripSuffix(".Value")))
          for {
            enum <- Option(module.instance).toSeq if enum.isInstanceOf[Enumeration]
            value <- enum.asInstanceOf[Enumeration].values.asInstanceOf[Iterable[Enumeration#Value]]
          } yield value.toString
        }.toOption.filterNot(_.isEmpty)
      } else None
    }
  }

  /**
    * Unapply the type by name and return the Enumeratum enum constants if those exist.
    */
  private object EnumeratumEnum {
    def unapply(className: String): Option[Seq[String]] = {
      (for {
        clazz <- Try(Class.forName(className + "$"))
        singleton <- Try(clazz.getField("MODULE$").get(clazz))
        values <- Try(singleton.getClass.getDeclaredField("values"))
        _ = values.setAccessible(true)
        entries <- Try(values
          .get(singleton)
          .asInstanceOf[Vector[_]]
          .map { item =>
            val entryName = Try(
              item.getClass.getMethod("entryName")
            ).getOrElse(item.getClass.getMethod("value"))
            entryName.setAccessible(true)
            entryName.invoke(item).asInstanceOf[String]
          }
          .toList)
      } yield entries).toOption
    }
  }

  private def referenceParamMF(implicit name: String): MappingFunction = {
    case tpe if isReference(tpe) => referenceParam(tpe)
  }

  def isReference(tpeName: String): Boolean = modelQualifier.isModel(tpeName)

  private def referenceParam(referenceType: String)(implicit name: String): GenSwaggerParameter =
    GenSwaggerParameter(name = name, required = true, referenceType = Some(referenceType))

  private def optionalParamMF(
      implicit name: String,
      default: Option[JsValue],
      description: Option[String],
      cl: ClassLoader
  ): MappingFunction = {
    case tpe if higherOrderType("Option", tpe, None).isDefined =>
      optionalParam(higherOrderType("Option", tpe, None).get)
  }

  private def optionalParam(optionalTpe: String)(
      implicit name: String,
      default: Option[JsValue],
      description: Option[String],
      cl: ClassLoader
  ): SwaggerParameter = {
    val asRequired = mapParam(
      typeName = optionalTpe,
      name = name,
      default = default.flatMap {
        // If `Some("None")`, then `variable: Option[T] ? = None` is specified. So `default` is treated as if it does not exist.
        case JsString("None") => None
        case json => Some(json)
      },
      description = description
    )
    asRequired.update(required = false, nullable = true, default = asRequired.default)
  }

  private def itemsParamMF(
      implicit name: String,
      default: Option[JsValue],
      description: Option[String],
      cl: ClassLoader
  ): MappingFunction = {
    case tpe if collectionItemType(tpe).isDefined =>
      // TODO: This could use a different type to represent ItemsObject(http://swagger.io/specification/#itemsObject),
      // since the structure is not quite the same, and still has to be handled specially in a json transform (see propWrites in SwaggerSpecGenerator)
      // However, that spec conflicts with example code elsewhere that shows other fields in the object, such as properties:
      // http://stackoverflow.com/questions/26206685/how-can-i-describe-complex-json-model-in-swagger
      updateOnlyGenParam(generalParamMF.apply("array"))(_.copy(
        items = Some(
          mapParam(
            typeName = collectionItemType(tpe).get,
            name = name,
            default = default,
            description = description
          )
        )
      ))
  }

  private def updateOnlyGenParam(param: SwaggerParameter)(update: GenSwaggerParameter => GenSwaggerParameter)
      : SwaggerParameter =
    param match {
      case p: GenSwaggerParameter => update(p)
      case _ => param
    }

  private def customMappingMF(implicit name: String, default: Option[JsValue]): MappingFunction =
    customMappings.map { mapping =>
      val re = StringContext(removeKnownPrefixes(mapping.`type`)).ci
      val mf: MappingFunction = {
        case re() =>
          CustomSwaggerParameter(
            name,
            mapping.specAsParameter,
            mapping.specAsProperty,
            default = default,
            required = default.isEmpty && mapping.required
          )
      }
      mf
    }
      // mapping を全てチェックする
      .foldLeft[MappingFunction](PartialFunction.empty)(_ orElse _)

  implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci: Regex = ("(?i)" + sc.parts.mkString).r
  }

}
