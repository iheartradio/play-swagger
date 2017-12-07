package com.iheart.playSwagger

import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.iheart.playSwagger.Domain._
import com.iheart.playSwagger.OutputTransformer.SimpleOutputTransformer
import play.api.libs.json._
import ResourceReader.read
import org.yaml.snakeyaml.Yaml
import SwaggerParameterMapper.mapParam
import scala.collection.immutable.ListMap
import play.routes.compiler._

import scala.util.{ Try, Success, Failure }

object SwaggerSpecGenerator {
  private val marker = "##"
  val customMappingsFileName = "swagger-custom-mappings"
  val baseSpecFileName = "swagger"
  def apply(nameTransformer: DefinitionNameTransformer, swaggerV3: Boolean, domainNameSpaces: String*)(implicit cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(PrefixDomainModelQualifier(domainNameSpaces: _*), nameTransformer, swaggerV3 = swaggerV3)
  }
  def apply(swaggerV3: Boolean, domainNameSpaces: String*)(implicit cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(PrefixDomainModelQualifier(domainNameSpaces: _*), new NoTransformer, swaggerV3 = swaggerV3)
  }
  def apply(nameTransformer: DefinitionNameTransformer, outputTransformers: Seq[OutputTransformer], domainNameSpaces: String*)(implicit cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(PrefixDomainModelQualifier(domainNameSpaces: _*), nameTransformer, outputTransformers = outputTransformers)
  }
  def apply(outputTransformers: Seq[OutputTransformer], domainNameSpaces: String*)(implicit cl: ClassLoader): SwaggerSpecGenerator = {
    SwaggerSpecGenerator(PrefixDomainModelQualifier(domainNameSpaces: _*), new NoTransformer, outputTransformers = outputTransformers)
  }

  case object MissingBaseSpecException extends Exception(s"Missing a $baseSpecFileName.yml or $baseSpecFileName.json to provide base swagger spec")
}

final case class SwaggerSpecGenerator(
  modelQualifier:        DomainModelQualifier      = PrefixDomainModelQualifier(),
  nameTransformer:       DefinitionNameTransformer = new NoTransformer,
  defaultPostBodyFormat: String                    = "application/json",
  outputTransformers:    Seq[OutputTransformer]    = Nil,
  swaggerV3:             Boolean                   = false)(implicit cl: ClassLoader) {
  import SwaggerSpecGenerator.{ customMappingsFileName, baseSpecFileName, MissingBaseSpecException }
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
    base:       JsObject = Json.obj()): Try[JsObject] = {

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
    loop("", routesFile).flatMap { data ⇒
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
    baseJson: JsObject                  = Json.obj()): JsObject = {
    val pathsJson = paths.values.reduce(_ ++ _)

    val refKey = "$ref"
    val mainRefs = (pathsJson ++ baseJson) \\ refKey
    val customMappingRefs = for {
      customMapping ← customMappings
      mappingsJson = customMapping.specAsProperty.toSeq ++ customMapping.specAsParameter
      ref ← mappingsJson.flatMap(_ \\ refKey)
    } yield ref
    val allRefs = mainRefs ++ customMappingRefs

    val definitions: List[Definition] = {
      val referredClasses: Seq[String] = for {
        refJson ← allRefs
        ref ← refJson.asOpt[String]
        className = ref.stripPrefix(referencePrefix)
        if modelQualifier.isModel(className)
      } yield className

      DefinitionGenerator(modelQualifier, customMappings, nameTransformer).allDefinitions(referredClasses)
    }

    val definitionsJson = JsObject(definitions.map(d ⇒ d.name → Json.toJson(d)))

    //TODO: remove hardcoded path
    val generatedTagsJson = JsArray(
      paths.keys
        //.filterNot(_ == RoutesFileReader.rootRoute)
        .map(tag ⇒ Json.obj("name" → tag)).toSeq)

    val tagsJson = mergeByName(generatedTagsJson, (baseJson \ "tags").asOpt[JsArray].getOrElse(JsArray()))

    val pathsAndDefinitionsJson = Json.obj(
      "paths" → pathsJson,
      if (swaggerV3) {
        "components" → Json.obj(
          "schemas" -> definitionsJson)
      } else {
        "definitions" → definitionsJson
      })

    pathsAndDefinitionsJson.deepMerge(baseJson) + ("tags" → tagsJson)
  }

  private def referencePrefix = if (swaggerV3) "#/components/schemas/" else "#/definitions/"

  private val refWrite = OWrites((refType: String) ⇒ Json.obj("$ref" → JsString(referencePrefix + refType)))

  import play.api.libs.functional.syntax._

  private lazy val genParamWrites: OWrites[GenSwaggerParameter] = {
    val under = if (swaggerV3) __ \ "schema" else __

    (
      (__ \ 'name).write[String] ~
      (__ \ "schema").writeNullable[String](refWrite) ~
      (under \ 'type).writeNullable[String] ~
      (under \ 'format).writeNullable[String] ~
      (__ \ 'required).write[Boolean] ~
      (under \ 'default).writeNullable[JsValue] ~
      (under \ 'example).writeNullable[JsValue] ~
      (under \ "items").writeNullable[SwaggerParameter](propWrites) ~
      (under \ "enum").writeNullable[Seq[String]])(unlift(GenSwaggerParameter.unapply))
  }

  private def customParamWrites(csp: CustomSwaggerParameter): List[JsObject] = {
    csp.specAsParameter match {
      case head :: tail ⇒
        def withPrefix(input: JsObject): JsObject = {
          if (swaggerV3) Json.obj("schema" -> input) else input
        }

        val under = if (swaggerV3) __ \ "schema" else __
        val w = (
          (__ \ 'name).write[String] ~
          (__ \ 'required).write[Boolean] ~
          (under \ 'default).writeNullable[JsValue])(
            (c: CustomSwaggerParameter) ⇒ (c.name, c.required, c.default))

        (w.writes(csp) ++ withPrefix(head)) :: tail
      case Nil ⇒ Nil
    }
  }

  private lazy val customPropWrites: Writes[CustomSwaggerParameter] = Writes { cwp ⇒
    (__ \ 'default).writeNullable[JsValue].writes(cwp.default) ++
      (cwp.specAsProperty orElse cwp.specAsParameter.headOption).getOrElse(Json.obj())
  }

  private lazy val propWrites: Writes[SwaggerParameter] = Writes {
    case g: GenSwaggerParameter    ⇒ genPropWrites.writes(g)
    case c: CustomSwaggerParameter ⇒ customPropWrites.writes(c)
  }

  private lazy val genPropWrites: Writes[GenSwaggerParameter] = (
    (__ \ 'type).writeNullable[String] ~
    (__ \ 'format).writeNullable[String] ~
    (__ \ 'default).writeNullable[JsValue] ~
    (__ \ 'example).writeNullable[JsValue] ~
    (__ \ "$ref").writeNullable[String] ~
    (__ \ "items").lazyWriteNullable[SwaggerParameter](propWrites) ~
    (__ \ "enum").writeNullable[Seq[String]])(p ⇒ (p.`type`, p.format, p.default, p.example, p.referenceType.map(referencePrefix + _), p.items, p.enum))

  implicit class PathAdditions(path: JsPath) {
    def writeNullableIterable[A <: Iterable[_]](implicit writes: Writes[A]): OWrites[A] =
      OWrites[A] { (a: A) ⇒
        if (a.isEmpty) Json.obj()
        else JsPath.createObj(path → writes.writes(a))
      }
  }

  private implicit val propertiesWriter: Writes[Seq[SwaggerParameter]] = Writes[Seq[SwaggerParameter]] { ps ⇒
    JsObject(ps.map(p ⇒ p.name → Json.toJson(p)(propWrites)))
  }

  private implicit val defFormat: Writes[Definition] = (
    (__ \ 'description).writeNullable[String] ~
    (__ \ 'properties).write[Seq[SwaggerParameter]] ~
    (__ \ 'required).writeNullable[Seq[String]])((d: Definition) ⇒ (d.description, d.properties, requiredProperties(d.properties)))

  private def requiredProperties(properties: Seq[SwaggerParameter]): Option[Seq[String]] = {
    val required = properties.filter(_.required).map(_.name)
    if (required.isEmpty) None else Some(required)
  }

  private lazy val defaultBase: JsObject = readYmlOrJson[JsObject](baseSpecFileName).getOrElse(throw MissingBaseSpecException)

  private lazy val customMappings: CustomMappings = {
    readYmlOrJson[CustomMappings](customMappingsFileName).getOrElse(Nil)
  }

  private def readYmlOrJson[T: Reads](fileName: String): Option[T] = {
    readCfgFile[T](s"$fileName.json") orElse readCfgFile[T](s"$fileName.yml")
  }

  private def mergeByName(base: JsArray, toMerge: JsArray): JsArray = {
    JsArray(base.value.map { bs ⇒
      val name = (bs \ "name").as[String]
      findByName(toMerge, name).fold(bs) { f ⇒ bs.as[JsObject] deepMerge f }
    } ++ toMerge.value.filter { tm ⇒
      (tm \ "name").validate[String].fold({ errors ⇒ true }, { name ⇒
        findByName(base, name).isEmpty
      })
    })
  }

  private def findByName(array: JsArray, name: String): Option[JsObject] =
    array.value.find(param ⇒ (param \ "name").asOpt[String].contains(name))
      .map(_.as[JsObject])

  private[playSwagger] def readCfgFile[T](name: String)(implicit fjs: Reads[T]): Option[T] = {
    Option(cl.getResource(name)).map { url ⇒
      val st = url.openStream()
      try {
        val ext = url.getFile.split("\\.").last
        ext match {
          case "json"  ⇒ Json.parse(st).as[T]
          //TODO: improve error handling
          case "yml"   ⇒ parseYaml(read(st).get.mkString("\n"))
          case unknown ⇒ throw new IllegalArgumentException(s"$name has an unsupported extension. Use either json or yml. ")
        }
      } finally {
        st.close()
      }
    }
  }

  private def parseYaml[T](yamlStr: String)(implicit fjs: Reads[T]): T = {
    val yaml = new Yaml()
    val map = yaml.load(yamlStr)
    val mapper = new ObjectMapper()
    val jsonString = mapper.writeValueAsString(map)
    Json.parse(jsonString).as[T]
  }

  private def paths(routes: Seq[Route], prefix: String, tag: Option[Tag]): JsObject = {
    JsObject {
      val endPointEntries = routes.flatMap(route ⇒ endPointEntry(route, prefix, tag))

      // maintain the routes order as per the original routing file
      val zgbp = endPointEntries.zipWithIndex.groupBy(_._1._1)
      import collection.mutable.LinkedHashMap
      val lhm = LinkedHashMap(zgbp.toSeq sortBy (_._2.head._2): _*)
      val gbp2 = lhm mapValues (_ map (_._1)) toSeq

      gbp2.toSeq.map(x ⇒ (x._1, x._2.map(_._2).reduce(_ deepMerge _)))
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
      val method = route.verb.value.toLowerCase
      Some(fullPath(prefix, inRoutePath) → Json.obj(method → endPointSpec(route, tag)))
    }
  }

  private[playSwagger] def fullPath(prefix: String, inRoutePath: String): String =
    if (prefix.endsWith("/") && inRoutePath == "/") //special case for "/p/" , "/"
      "/" + prefix.stripPrefix("/")
    else
      "/" + List(
        prefix.stripPrefix("/").stripSuffix("/"),
        inRoutePath.stripPrefix("/")).filterNot(_.isEmpty).
        mkString("/")

  // Multiple routes may have the same path, merge the objects instead of overwriting

  private def endPointSpec(route: Route, tag: Option[String]) = {

    def tryParseYaml(comment: String): Option[JsObject] = {
      val pattern = "^\\w+:".r
      pattern.findFirstIn(comment).map(_ ⇒ parseYaml[JsObject](comment))
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

      val params = for {
        paramList ← route.call.parameters.toSeq
        param ← paramList
        if param.fixed.isEmpty // Removes parameters the client cannot set
      } yield mapParam(param, nameTransformer, modelQualifier, customMappings)

      JsArray(params.flatMap { p ⇒
        val jos: List[JsObject] = p match {
          case gsp: GenSwaggerParameter    ⇒ List(genParamWrites.writes(gsp))
          case csp: CustomSwaggerParameter ⇒ customParamWrites(csp)
        }

        val in = if (pathParams.contains(p.name)) "path" else "query"
        val enhance = Json.obj("in" → in)
        jos.map(enhance ++ _)
      })
    }

    def amendBodyParam(params: JsArray): JsArray = {
      val bodyParam = findByName(params, "body")
      bodyParam.fold(params) { param ⇒
        val enhancedBodyParam = Json.obj("in" → JsString("body")) ++ param
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

    val operationId = Json.obj(
      "operationId" → route.call.method)

    val rawPathJson = operationId ++ tag.fold(Json.obj()) { t ⇒
      Json.obj("tags" → List(t))
    } ++ jsonFromComment.getOrElse(Json.obj()) ++ parameterJson

    val hasConsumes = (rawPathJson \ "consumes").toOption.isDefined

    if (findByName(mergedParams, "body").isDefined && !hasConsumes)
      rawPathJson + ("consumes" → Json.arr(defaultPostBodyFormat))
    else rawPathJson
  }
}
