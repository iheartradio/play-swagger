package com.iheart.playSwagger.generator

import java.io.File

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

import com.iheart.playSwagger.OutputTransformer.SimpleOutputTransformer
import com.iheart.playSwagger._
import com.iheart.playSwagger.domain.parameter.{CustomSwaggerParameter, GenSwaggerParameter, SwaggerParameterWriter}
import com.iheart.playSwagger.domain.{CustomTypeMapping, Definition}
import com.iheart.playSwagger.exception.RoutesParseException.RoutesParseErrorDetail
import com.iheart.playSwagger.exception.{MissingBaseSpecException, RoutesParseException}
import com.iheart.playSwagger.generator.ResourceReader.read
import com.iheart.playSwagger.generator.SwaggerSpecGenerator._
import com.iheart.playSwagger.generator.YAMLParser.parseYaml
import com.iheart.playSwagger.util.ExtendJsValue.JsObjectUpdate
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json._
import play.routes.compiler._

object SwaggerSpecGenerator {
  private val defaultRoutesFile = "routes"
  private val routesExt = ".routes"
  private val skipFileHeader = "### SkipFileForDocs ###"
  private val swaggerCommentMarker = "##"
  private val skipPathCommentRegex = ("##\\s*NoDocs\\s*##").r
  private val customMappingsFileName = "swagger-custom-mappings"

  /** $ref */
  private val refKey = "$ref"
  private val baseSpecFileName = "swagger"

  def apply(namingConvention: NamingConvention, swaggerV3: Boolean, domainNameSpaces: String*)(implicit
  cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(
      namingConvention = namingConvention,
      modelQualifier = PrefixDomainModelQualifier(domainNameSpaces: _*),
      swaggerV3 = swaggerV3
    )
  }

  def apply(
      namingConvention: NamingConvention,
      outputTransformers: Seq[OutputTransformer],
      domainNameSpaces: String*
  )(implicit cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(
      namingConvention = namingConvention,
      modelQualifier = PrefixDomainModelQualifier(domainNameSpaces: _*),
      outputTransformers = outputTransformers
    )
  }

  def apply(swaggerV3: Boolean, operationIdFully: Boolean, embedScaladoc: Boolean, domainNameSpaces: String*)(implicit
  cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(
      namingConvention = NamingConvention.None,
      modelQualifier = PrefixDomainModelQualifier(domainNameSpaces: _*),
      swaggerV3 = swaggerV3,
      operationIdFully = operationIdFully,
      embedScaladoc = embedScaladoc
    )
  }

  def apply(outputTransformers: Seq[OutputTransformer], domainNameSpaces: String*)(implicit
  cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(
      namingConvention = NamingConvention.None,
      modelQualifier = PrefixDomainModelQualifier(domainNameSpaces: _*),
      outputTransformers = outputTransformers
    )
  }

}

/**
  * @param namingConvention      命名規則 (snake_case, camelCase など)
  * @param modelQualifier        ドメインモデル判定器
  * @param defaultPostBodyFormat 未記載の場合の Post レスポンスボディの MIME TYPE
  * @param apiVersion            対象 API のバージョン
  * @param operationIdFully      API の名前にパッケージ名を利用するか
  */
final case class SwaggerSpecGenerator(
    namingConvention: NamingConvention = NamingConvention.None,
    modelQualifier: DomainModelQualifier = PrefixDomainModelQualifier(),
    defaultPostBodyFormat: String = "application/json",
    outputTransformers: Seq[OutputTransformer] = Nil,
    swaggerV3: Boolean = false,
    swaggerPlayJava: Boolean = false,
    apiVersion: Option[String] = None,
    operationIdFully: Boolean = false,
    embedScaladoc: Boolean = false
)(implicit cl: ClassLoader) {

  private val parameterWriter = new SwaggerParameterWriter(swaggerV3)

  private def readYmlOrJson[T: Reads](fileName: String): Option[T] = {
    readCfgFile[T](s"$fileName.json") orElse readCfgFile[T](s"$fileName.yml")
  }

  private lazy val customMappings: Seq[CustomTypeMapping] = {
    readYmlOrJson[Seq[CustomTypeMapping]](customMappingsFileName).getOrElse(Nil)
  }

  private lazy val swaggerParameterMapper = new SwaggerParameterMapper(customMappings, modelQualifier)
  private lazy val definitionGenerator = DefinitionGenerator(
    mapper = swaggerParameterMapper,
    swaggerPlayJava = swaggerPlayJava,
    namingConvention = namingConvention,
    embedScaladoc = embedScaladoc
  )

  // routes with their prefix
  type Routes = (Path, Seq[Route])
  type Tag = String
  type Path = String

  // Mapping of the tag, which is the file the routes were read from, and the optional prefix if it was
  // included from another router. ListMap is used to maintain the original definition order
  type RoutesData = Try[ListMap[Tag, Routes]]

  def generate(routesFile: String = defaultRoutesFile): Try[JsObject] = {
    val base = apiVersion.fold(defaultBase) { v =>
      // version を base json にマージする
      Json.obj("info" -> Json.obj("version" -> v)) deepMerge defaultBase
    }
    generateFromRoutesFile(routesFile = routesFile, base = base)
  }

  /** .routes を除いたファイル名がタグ名となる */
  private def tagFromFile(fileName: String): Tag = fileName.replace(routesExt, "")

  private def loop(path: Path, routesFile: String): RoutesData = {
    // TODO: better error handling
    ResourceReader.read(routesFile).flatMap { lines =>
      lines.headOption match {
        // ドキュメントの第一行に設定が記載されている場合はスキップする
        case Some(SwaggerSpecGenerator.skipFileHeader) => Success(ListMap.empty)
        case _ =>
          val content = lines.mkString("\n")

          // artificial file to conform to api, used by play for error reporting
          val file = new File(routesFile)

          RoutesFileParser.parseContent(content, file).fold(
            // パースに失敗した場合
            { errors =>
              val detail = errors.map { error =>
                val lineNumber = error.line
                val column = error.column
                val errorLine = lineNumber.flatMap(line => lines.lift(line - 1))
                RoutesParseErrorDetail(error.source.getName, error.message, errorLine, lineNumber, column)
              }
              Failure(new RoutesParseException(detail))
            },
            { rules: Seq[Rule] =>
              val tag = tagFromFile(routesFile)
              val init: RoutesData = Success(ListMap(tag -> (path, Seq.empty)))
              rules.foldLeft(init) {
                // Route 内に直接 API 定義がある場合
                case (Success(routesData), route: Route) =>
                  // 定義済みの routes 情報とマージする
                  // 例えば、1回目の実行では `routes` ファイルの内容が展開されるため、 prefix には " " が代入される
                  val (prefix, routes) = routesData(tag)
                  Success(routesData + (tag -> (prefix, routes :+ route)))
                // 他の Routes ファイルへの参照がある場合
                case (Success(routesData), Include(prefix, router)) =>
                  val referenceFile = router.replace(".Routes", ".routes")
                  val isIncludedRoutesFile = cl.getResource(referenceFile) != null
                  if (!isIncludedRoutesFile) {
                    Success(routesData)
                  } else {
                    // routes ファイルが入れ子になった場合、親の path とそのファイルの path をマージする
                    val updatedPath = if (path.nonEmpty) path + "/" + prefix else prefix
                    loop(updatedPath, referenceFile).map(routesData ++ _)
                  }
                // 失敗した場合はそこで中断
                case (l: Failure[_], _) => l
              }
            }
          )
      }
    }
  }

  private[generator] def generateFromRoutesFile(
      routesFile: String,
      base: JsObject
  ): Try[JsObject] = {

    // starts with empty prefix, assuming that the routesFile is the outermost (usually 'routes')
    loop("", routesFile).flatMap { data =>
      val result: JsObject = generateFromRoutes(data, base)
      val initial = SimpleOutputTransformer(Success[JsObject])
      val mapper = outputTransformers.foldLeft[OutputTransformer](initial)(_ >=> _)
      mapper(result)
    }
  }

  /**
    * Generate directly from routes
    *
    * @param routes [[Route]]s compiled by Play routes compiler
    * @param base   swagger.yaml に記載された基本設定
    */
  private def generateFromRoutes(routes: ListMap[Tag, (String, Seq[Route])], base: JsObject): JsObject = {
    val docs = routes.map {
      case (tag, (path, routes)) =>
        tag -> paths(routes, path, Some(tag))
    }.filter(_._2.keys.nonEmpty)
    generateWithBase(docs, base)
  }

  /** 基本の swagger.yaml とマージして、 #/definitions で参照される case class から定義を生成する */
  private[playSwagger] def generateWithBase(
      paths: ListMap[Tag, JsObject],
      baseJson: JsObject = Json.obj()
  ): JsObject = {

    // 1つの Json にまとめる
    val pathsJson = paths.values.reduce((acc, p) => JsObject(acc.fields ++ p.fields))

    // $ref: として定義されている名前を一覧で取得する
    val mainRefs = (pathsJson ++ baseJson) \\ refKey

    // swagger-custom-mappings.yaml で指定される $ref: の一覧を取得する
    val customMappingRefs = for {
      customMapping <- customMappings
      mappingsJson = customMapping.specAsProperty.toSeq ++ customMapping.specAsParameter
      ref <- mappingsJson.flatMap(_ \\ refKey)
    } yield ref
    val allRefs = mainRefs ++ customMappingRefs

    // $ref: で参照される名前から定義リストを作成する。
    val definitions: List[Definition] = {
      val referredClasses: Seq[String] = for {
        refJson <- allRefs.toList
        ref <- refJson.asOpt[String].toList
        // #/definitions を省いたものがクラス名
        className = ref.stripPrefix(parameterWriter.referencePrefix)
        if modelQualifier.isModel(className)
      } yield className

      definitionGenerator.allDefinitions(referredClasses)
    }

    val definitionsJson =
      JsObject(definitions.map(d => d.name -> Json.toJson(d)(Definition.writer(parameterWriter.propertiesWriter))))

    val pathsAndDefinitionsJson = Json.obj(
      "paths" -> pathsJson,
      if (swaggerV3) {
        "components" -> Json.obj(
          "schemas" -> definitionsJson
        )
      } else {
        "definitions" -> definitionsJson
      },
      // base json に tags が存在しない場合
      "tags" -> JsArray()
    )

    pathsAndDefinitionsJson.deepMerge(baseJson)
  }

  private lazy val defaultBase: JsObject =
    readYmlOrJson[JsObject](baseSpecFileName).getOrElse(throw new MissingBaseSpecException(baseSpecFileName))

  private def mergeByName(base: JsArray, toMerge: JsArray): JsArray = {
    JsArray(base.value.map { bs =>
      val name = (bs \ "name").as[String]
      findByName(toMerge, name).fold(bs) { f => bs.as[JsObject] deepMerge f }
    } ++ toMerge.value.filter { tm =>
      (tm \ "name").validate[String].fold(
        { _ => true },
        { name =>
          findByName(base, name).isEmpty
        }
      )
    })
  }

  private def findByName(array: JsArray, name: String): Option[JsObject] =
    array.value.find(param => (param \ "name").asOpt[String].contains(name))
      .map(_.as[JsObject])

  private[playSwagger] def readCfgFile[T](name: String)(implicit fjs: Reads[T]): Option[T] = {
    Option(cl.getResource(name)).map { url =>
      val st = url.openStream()
      try {
        val ext = url.getFile.split("\\.").last
        ext match {
          case "json" => Json.parse(st).as[T]
          // TODO: improve error handling
          case "yml" => YAMLParser.parseYaml(read(st).get.mkString("\n"))
          case _ =>
            throw new IllegalArgumentException(s"$name has an unsupported extension. Use either json or yml. ")
        }
      } finally {
        st.close()
      }
    }
  }

  private def paths(routes: Seq[Route], path: String, tag: Option[Tag]): JsObject = {
    JsObject {
      val endPointEntries = routes.flatMap(route => endPointEntry(route, path, tag))

      // maintain the routes order as per the original routing file
      val zgbp = endPointEntries.zipWithIndex.groupBy(_._1._1)
      val lhm = mutable.LinkedHashMap(zgbp.toSeq.sortBy(_._2.head._2): _*)
      val gbp2 = lhm.mapValues(_.map(_._1)).toSeq

      gbp2.map(x => (x._1, x._2.map(_._2).reduce(_ deepMerge _)))
    }
  }

  private def endPointEntry(route: Route, path: String, tag: Option[String]): Option[(String, JsObject)] = {

    val comments = route.comments.map(_.comment).mkString("\n")
    // NoDocs がついている path は無視する
    if (skipPathCommentRegex.findFirstIn(comments).isDefined) {
      None
    } else {
      val inRoutePath = route.path.parts.map {
        // パスパラメータの場合は {} で囲む
        case DynamicPart(name, _, _) => s"{$name}"
        // StaticPart には前後の "/" が含まれる
        case StaticPart(value) => value
      }.mkString
      val method = route.verb.value.toLowerCase
      Some(fullPath(path, inRoutePath) -> Json.obj(method -> endPointSpec(route, tag)))
    }
  }

  /** routes ファイルとコントローラーの path をマージする */
  private[playSwagger] def fullPath(path: String, inRoutePath: String): String = {
    // special case for ("/p/" , "/") or ("/p/" , "")
    if (path.endsWith("/") && (inRoutePath == "/" || inRoutePath.isEmpty)) { // special case for ("/p/" , "/") or ("/p/" , "")
      "/" + path.stripPrefix("/")
    } else {
      "/" + List(
        path.stripPrefix("/").stripSuffix("/"),
        inRoutePath.stripPrefix("/")
      ).filterNot(_.isEmpty).mkString("/")
    }
  }

  // Multiple routes may have the same path, merge the objects instead of overwriting
  private def endPointSpec(route: Route, tag: Option[String]) = {
    // controller から parameter object の作成
    val paramsFromController = {
      val pathParams = route.path.parts.collect {
        case d: DynamicPart => d.name
      }.toSet

      val params = for {
        paramList <- route.call.parameters.toSeq
        param <- paramList
        if param.fixed.isEmpty && !param.isJavaRequest // Removes parameters the client cannot set
      } yield swaggerParameterMapper.mapParam(param, None)

      JsArray(params.flatMap { p =>
        val jos: List[JsObject] = p match {
          case gsp: GenSwaggerParameter => List(parameterWriter.genParamWrites.writes(gsp))
          case csp: CustomSwaggerParameter => parameterWriter.customParamWrites(csp)
        }

        val in = if (pathParams.contains(p.name)) "path" else "query"
        val enhance = Json.obj("in" -> in)
        jos.map(enhance ++ _)
      })
    }

    // コメントから parameter object の作成
    val jsonFromComment = {
      val comments = route.comments.map(_.comment)
      val commentDocLines = comments match {
        case SwaggerSpecGenerator.swaggerCommentMarker +: docs :+ SwaggerSpecGenerator.swaggerCommentMarker => docs
        case _ => Nil
      }

      val commentsJsonOpt = for {
        leadingSpace <- commentDocLines.headOption.flatMap("""^(\s*)""".r.findFirstIn)
        comment = commentDocLines.map(_.drop(leadingSpace.length)).mkString("\n")
        result <- tryParseJson(comment) orElse tryParseYaml(comment)
      } yield result

      commentsJsonOpt.map { commentsJson =>
        JsObject(commentsJson.update(refKey) {
          case JsString(v) =>
            val pattern = "^([^#]+)(?:#(?:/[a-zA-Z])+)?$".r
            v match {
              // #/definitions/ のようなものが指定されて**いない**場合はファイルへのリンクとして取得を試みる
              case pattern(path) if PathValidator.isValid(path) =>
                readCfgFile[JsObject](path).getOrElse(JsObject(Seq(refKey -> JsString(v))))
              case _ => JsObject(Seq(refKey -> JsString(v)))
            }
          case v => JsObject(Seq(refKey -> v))
        })
      }
    }

    val paramsFromComment = jsonFromComment.flatMap(jc => (jc \ "parameters").asOpt[JsArray]).map { params =>
      // play-swagger 仕様としてボディパラメータでの ref の使用は `name: body` が利用される
      val bodyParam = findByName(params, "body")
      bodyParam.fold(params) { param =>
        // 本来は `in: body` の後に型の定義が続く形式
        val enhancedBodyParam = Json.obj("in" -> JsString("body")) ++ param
        JsArray(enhancedBodyParam +: params.value.filterNot(_ == bodyParam.get))
      }
    }

    val mergedParams = mergeByName(paramsFromController, paramsFromComment.getOrElse(JsArray()))

    val parameterJson = if (mergedParams.value.nonEmpty) Json.obj("parameters" -> mergedParams) else Json.obj()

    // コントローラー名とメソッド名、もしくはメソッド名のみから operationId を取得する
    val operationId = Json.obj(
      "operationId" -> (if (operationIdFully) s"${route.call.controller}.${route.call.method}" else route.call.method)
    )

    // operationId, tag, parameter object, コメントから生成されたその他の情報をマージする
    val rawPathJson = operationId ++ tag.fold(Json.obj()) { t =>
      Json.obj("tags" -> List(t))
    } ++ jsonFromComment.getOrElse(Json.obj()) ++ parameterJson

    val hasConsumes = (rawPathJson \ "consumes").toOption.isDefined

    // MIME Type の指定がない場合はデフォルトを設定する
    if (findByName(mergedParams, "body").isDefined && !hasConsumes)
      rawPathJson + ("consumes" -> Json.arr(defaultPostBodyFormat))
    else rawPathJson
  }

  private def tryParseYaml(comment: String): Option[JsObject] = {
    // The purpose here is more to ensure that it is not in other formats such as JSON
    // If invalid YAML is passed, org.yaml.snakeyaml.parser.ParserException
    val pattern = "^\\w+|\\$ref:".r
    pattern.findFirstIn(comment).map(_ => parseYaml[JsObject](comment))
  }

  private def tryParseJson(comment: String): Option[JsObject] =
    if (comment.startsWith("{")) Some(Json.parse(comment).as[JsObject]) else None

}
