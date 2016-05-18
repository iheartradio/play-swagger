package com.iheart.playSwagger

import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.iheart.playSwagger.Domain.{Definition, SwaggerParameter}
import play.api.libs.json._
import ResourceReader.read
import play.api.libs.functional.syntax._
import org.yaml.snakeyaml.Yaml
import SwaggerParameterMapper.mapParam
import scala.collection.immutable.ListMap
import play.routes.compiler._

import scala.util.{Try, Success, Failure}

object SwaggerSpecGenerator {
  private val marker = "##"
  def apply(domainNameSpaces: String*)(implicit cl: ClassLoader): SwaggerSpecGenerator = SwaggerSpecGenerator(DomainModelQualifier(domainNameSpaces: _*))
}

final case class SwaggerSpecGenerator(
  modelQualifier:        DomainModelQualifier = DomainModelQualifier(),
  defaultPostBodyFormat: String               = "application/json"
)(implicit cl: ClassLoader) {

  // routes with their prefix
  type Routes = (String, Seq[Route])

  // Mapping of the tag, which is the file the routes were read from, and the optional prefix if it was
  // included from another router. ListMap is used to maintain the original definition order
  type RoutesData = Try[ListMap[Tag, Routes]]

  val defaultRoutesFile = "routes"

  def generate(routesFile: String = defaultRoutesFile): Try[JsObject] = generateFromRoutesFile(routesFile = routesFile, base = defaultBase)

  val routesExt = ".routes"

  private[playSwagger] def generateFromRoutesFile(
    routesFile: String   = defaultRoutesFile,
    base:       JsObject = Json.obj()
  ): Try[JsObject] = {

    def tagFromFile(file: String) = file.replace(routesExt, "")

    def loop(path: String, routesFile: String): RoutesData = {

      // TODO: better error handling
      ResourceReader.read(routesFile).flatMap { lines ⇒
        val content = lines.mkString("\n")
        // artificial file to conform to api, used by play for error reporting
        val file = new File(routesFile)

        def errorMessage(error: RoutesCompilationError) = {
          val lineNumber = error.line.fold("")(":" + _ + error.column.fold("")(":" + _))
          val errorLine = error.line.flatMap { line ⇒
            val caret = error.column.map(c ⇒ (" " * (c - 1)) + "^").getOrElse("")
            lines.lift(line - 1).map(_ + "\n" + caret)
          }.getOrElse("")
          s"""|Error parsing routes file: ${error.source.getName}$lineNumber ${error.message}
              |$errorLine
              |""".stripMargin
        }

        RoutesFileParser.parseContent(content, file).fold({ errors ⇒
          val message = errors.map(errorMessage).mkString("\n")
          Failure(new Exception(message))
        }, { rules ⇒
          val routerName = tagFromFile(routesFile)
          val init: RoutesData = Success(ListMap(routerName → (path, Seq.empty)))
          rules.foldLeft(init) {
            case (Success(acc), route: Route) ⇒
              val (prefix, routes) = acc(routerName)
              Success(acc + (routerName → (prefix, routes :+ route)))
            case (Success(acc), Include(prefix, router)) ⇒
              val reference = router.replace(".Routes", ".routes")
              val isIncludedRoutesFile = cl.getResource(reference) != null
              if (isIncludedRoutesFile) {
                val updated = if (path.nonEmpty) path + "/" + prefix else prefix
                loop(updated, reference).map(acc ++ _)
              } else Success(acc)

            case (l @ Failure(_), _) ⇒ l
          }
        })
      }
    }

    // starts with empty prefix, assuming that the routesFile is the outermost (usually 'routes')
    loop("", routesFile).map(generateFromRoutes(_, base))
  }

  /**
   * Generate directly from routes
   *
   * @param routes [[Route]]s compiled by Play routes compiler
   * @param base
   * @return
   */
  def generateFromRoutes(routes: ListMap[Tag, (String, Seq[Route])], base: JsObject = defaultBase): JsObject = {
    val docs = routes.map {
      case (tag, (prefix, routes)) ⇒
        //val subTag = if (tag == tagFromFile(routesFile)) None else Some(tag)
        tag → paths(routes, prefix, Some(tag))
    }.filter(_._2.keys.nonEmpty)
    generateWithBase(docs, base)
  }

  private[playSwagger] def generateWithBase(
    paths:    ListMap[String, JsObject],
    baseJson: JsObject                  = Json.obj()
  ): JsObject = {
    val pathsJson = paths.values.reduce(_ ++ _)
    val allRefs = (pathsJson ++ baseJson) \\ "$ref"

    val definitions: List[Definition] = {
      val referredClasses: Seq[String] = for {
        refJson ← allRefs
        ref ← refJson.asOpt[String]
        className = ref.stripPrefix(referencePrefix)
        if modelQualifier.isModel(className)
      } yield className

      DefinitionGenerator(modelQualifier).allDefinitions(referredClasses)
    }

    val definitionsJson = JsObject(definitions.map(d ⇒ d.name → Json.toJson(d)))

    //TODO: remove hardcoded path
    val generatedTagsJson = JsArray(
      paths.keys
      //.filterNot(_ == RoutesFileReader.rootRoute)
      .map(tag ⇒ Json.obj("name" → tag)).toSeq
    )

    val tagsJson = mergeByName(generatedTagsJson, (baseJson \ "tags").asOpt[JsArray].getOrElse(JsArray()))

    Json.obj(
      "paths" → pathsJson,
      "definitions" → definitionsJson
    ).deepMerge(baseJson) + (
        "tags" → tagsJson
      )
  }

  private val referencePrefix = "#/definitions/"

  private val refWrite = OWrites((refType: String) ⇒ Json.obj("$ref" → JsString(referencePrefix + refType)))

  import play.api.libs.functional.syntax._

  private lazy val paramFormat: Writes[SwaggerParameter] = (
    (__ \ 'name).write[String] ~
    (__ \ 'type).writeNullable[String] ~
    (__ \ 'format).writeNullable[String] ~
    (__ \ 'required).write[Boolean] ~
    (__ \ 'default).writeNullable[JsValue] ~
    (__ \ 'example).writeNullable[JsValue] ~
    (__ \ "schema").writeNullable[String](refWrite) ~
    (__ \ "items").lazyWriteNullable[SwaggerParameter](defPropFormat.transform((js: JsValue) ⇒ transformItems(js))) ~
    (__ \ "enum").writeNullable[Seq[String]]
  )(unlift(SwaggerParameter.unapply))

  private lazy val defPropFormat: Writes[SwaggerParameter] = (
    (__ \ 'type).writeNullable[String] ~
    (__ \ 'format).writeNullable[String] ~
    (__ \ 'required).write[Boolean] ~
    (__ \ 'default).writeNullable[JsValue] ~
    (__ \ 'example).writeNullable[JsValue] ~
    (__ \ "schema").writeNullable[String](refWrite) ~
    (__ \ "items").lazyWriteNullable[SwaggerParameter](defPropFormat.transform((js: JsValue) ⇒ transformItems(js))) ~
    (__ \ "enum").writeNullable[Seq[String]]
  )(p ⇒ (p.`type`, p.format, p.required, p.default, p.example, p.referenceType, p.items, p.enum))

  implicit class PathAdditions(path: JsPath) {
    def writeNullableIterable[A <: Iterable[_]](implicit writes: Writes[A]): OWrites[A] =
      OWrites[A] { (a: A) ⇒
        if (a.isEmpty) Json.obj()
        else JsPath.createObj(path → writes.writes(a))
      }
  }

  private def transformItems(js: JsValue): JsValue = {
    val filtered = js.as[JsObject] - "name"
    val movedRef = (filtered \ "schema" \ "$ref").asOpt[JsValue].fold(filtered) {
      (filtered - "schema") + "$ref" → _
    }
    (movedRef \ "items").asOpt[JsValue].fold(movedRef) {
      movedRef + "items" → _
    }
  }

  private implicit val swesWriter: Writes[Seq[SwaggerParameter]] = Writes[Seq[SwaggerParameter]] { ps ⇒
    JsObject(ps.map(p ⇒ p.name → Json.toJson(p)(defPropFormat)))
  }

  private implicit val defFormat: Writes[Definition] = (
    (__ \ 'description).writeNullable[String] ~
    (__ \ 'properties).write[Seq[SwaggerParameter]] ~
    (__ \ 'required).write[Seq[String]]
  )((d: Definition) ⇒ (d.description, d.properties, d.properties.filter(_.required).map(_.name)))

  private def defaultBase = readBaseCfg("swagger.json") orElse readBaseCfg("swagger.yml") getOrElse Json.obj()

  private def mergeByName(base: JsArray, toMerge: JsArray): JsArray = {
    JsArray(base.value.map { bs ⇒
      val name = (bs \ "name").as[String]
      findByName(toMerge, name).fold(bs) { f ⇒ bs.as[JsObject] deepMerge f }
    } ++ toMerge.value.filter { tm ⇒
      val name = (tm \ "name").as[String]
      findByName(base, name).isEmpty
    })
  }

  private def findByName(array: JsArray, name: String): Option[JsObject] =
    array.value.find(param ⇒ (param \ "name").asOpt[String].contains(name))
      .map(_.as[JsObject])

  private def readBaseCfg(name: String): Option[JsObject] = {
    Option(cl.getResource(name)).map { url ⇒
      val st = url.openStream()
      try {
        val ext = url.getFile.split("\\.").last
        ext match {
          case "json"  ⇒ Json.parse(st).as[JsObject]
          //TODO: improve error handling
          case "yml"   ⇒ parseYaml(read(st).get.mkString("\n"))
          case unknown ⇒ throw new IllegalArgumentException(s"$name has an unsupported extension. Use either json or yaml. ")
        }
      } finally {
        st.close()
      }
    }
  }

  private def parseYaml(yamlStr: String): JsObject = {
    val yaml = new Yaml()
    val map = yaml.load(yamlStr).asInstanceOf[java.util.Map[String, Object]]
    val mapper = new ObjectMapper()
    val jsonString = mapper.writeValueAsString(map)
    Json.parse(jsonString).as[JsObject]
  }

  private[playSwagger] def paths(routes: Seq[Route], prefix: String, tag: Option[Tag]): JsObject = {
    JsObject {
      routes.flatMap(endPointEntry(_, prefix, tag))
        .groupBy(_._1) // Routes grouped by path
        .mapValues(_.map(_._2).reduce(_ deepMerge _))
    }
  }

  private def endPointEntry(route: Route, prefix: String, tag: Option[String]): Option[(String, JsObject)] = {
    import SwaggerSpecGenerator.marker

    val comments = route.comments.map(_.comment).mkString("\n")
    if (s"$marker\\s*NoDocs\\s*$marker".r.findFirstIn(comments).isDefined) {
      None
    } else {
      val inRoutePath = route.path.parts.map {
        case DynamicPart(name, _, _) ⇒ s"{$name}"
        case StaticPart(value)       ⇒ value
      }.mkString
      val path = "/" + List(prefix, inRoutePath).filterNot(p ⇒ p.isEmpty || p == "/").mkString("/")
      val method = route.verb.value.toLowerCase
      Some(path → Json.obj(method → endPointSpec(route, tag)))
    }
  }

  // Multiple routes may have the same path, merge the objects instead of overwriting

  private def endPointSpec(route: Route, tag: Option[String]) = {

    def tryParseYaml(comment: String): Option[JsObject] = {
      val pattern = "^\\w+:".r
      pattern.findFirstIn(comment).map(_ ⇒ parseYaml(comment))
    }

    def tryParseJson(comment: String): Option[JsObject] = {
      if (comment.startsWith("{"))
        Some(Json.parse(comment).as[JsObject])
      else None
    }

    val paramsFromController = {
      val pathParams = route.path.parts.collect {
        case d: DynamicPart ⇒ d.name
      }.toSet

      val params = route.call.parameters
        .fold(Seq.empty[SwaggerParameter])(_.map(mapParam(_, modelQualifier)))

      JsArray(params.map { p ⇒
        val jo = Json.toJson(p)(paramFormat).as[JsObject]
        val in = if (pathParams.contains(p.name)) "path" else "query"
        jo + ("in" → JsString(in))
      })
    }

    def amendBodyParam(params: JsArray): JsArray = {
      val bodyParam = findByName(params, "body")
      bodyParam.fold(params) { param ⇒
        val enhancedBodyParam = param + ("in" → JsString("body"))
        JsArray(enhancedBodyParam +: params.value.filterNot(_ == bodyParam.get))
      }
    }

    val jsonFromComment = {
      import SwaggerSpecGenerator.marker

      val comments = route.comments.map(_.comment)
      val commentDocLines = comments match {
        case `marker` +: docs :+ `marker` ⇒ docs
        case _                            ⇒ Nil
      }

      for {
        leadingSpace ← commentDocLines.headOption.flatMap("""^(\s*)""".r.findFirstIn)
        comment = commentDocLines.map(_.drop(leadingSpace.length)).mkString("\n")
        result ← tryParseJson(comment) orElse tryParseYaml(comment)
      } yield result
    }

    val paramsFromComment = jsonFromComment.flatMap(jc ⇒ (jc \ "parameters").asOpt[JsArray]).map(amendBodyParam)

    val mergedParams = mergeByName(paramsFromController, paramsFromComment.getOrElse(JsArray()))

    val parameterJson = if (mergedParams.value.nonEmpty) Json.obj("parameters" → mergedParams) else Json.obj()

    val rawPathJson = tag.fold(Json.obj()) { t ⇒
      Json.obj("tags" → List(t))
    } ++ jsonFromComment.getOrElse(Json.obj()) ++ parameterJson

    val hasConsumes = (rawPathJson \ "consumes").toOption.isDefined

    if (findByName(mergedParams, "body").isDefined && !hasConsumes)
      rawPathJson + ("consumes" → Json.arr(defaultPostBodyFormat))
    else rawPathJson
  }
}

