package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json._

case class Track(name: String, genre: Option[String], artist: Artist, related: Seq[Artist], numbers: Seq[Int])
case class Artist(name: String, age: Int)

case class Student(name: String, teacher: Option[Teacher])
case class Teacher(name: String)

case class PolymorphicContainer(item: PolymorphicItem)
trait PolymorphicItem

case class JavaEnumContainer(status: SampleJavaEnum)

case class AllOptional(a: Option[String], b: Option[String])

class SwaggerSpecGeneratorSpec extends Specification {
  implicit val cl = getClass.getClassLoader

  "full path" >> {
    val gen = SwaggerSpecGenerator()
    "combine routePath with prefix" >> {
      gen.fullPath("/p", "d") === "/p/d"
    }

    "ignore prefix when it's just a /" >> {
      gen.fullPath("/", "d") === "/d"
    }

    "avoid double slash at the beginning" >> {
      gen.fullPath("/", "/d") === "/d"
    }

    "avoid double slash at the middle" >> {
      gen.fullPath("p/", "/d") === "/p/d"
    }

    "allow internal double slash" >> {
      gen.fullPath("p/", "/d//c") === "/p/d//c"
    }

    "respect top level trailing slash" >> {
      gen.fullPath("p/", "/") === "/p/"
    }

  }

}

class SwaggerSpecGeneratorIntegrationSpec extends Specification {
  implicit val cl = getClass.getClassLoader

  "integration" >> {

    lazy val defaultRoutesFile = SwaggerSpecGenerator(ExcludingDomainQualifier).generate()

    "Use default routes file when no argument is given" >> {
      val json = defaultRoutesFile.get
      (json \ "paths" \ "/player/{pid}/context/{bid}").asOpt[JsObject] must beSome
    }

    lazy val json = SwaggerSpecGenerator("com.iheart").generate("test.routes").get
    lazy val pathJson = json \ "paths"
    lazy val definitionsJson = json \ "definitions"
    lazy val postBodyJson = (pathJson \ "/post-body" \ "post").as[JsObject]
    lazy val artistJson = (pathJson \ "/api/artist/{aid}/playedTracks/recent" \ "get").as[JsObject]
    lazy val stationJson = (pathJson \ "/api/station/{sid}/playedTracks/last" \ "get").as[JsObject]
    lazy val addTrackJson = (pathJson \ "/api/station/playedTracks" \ "post").as[JsObject]
    lazy val playerJson = (pathJson \ "/api/player/{pid}/context/{bid}" \ "get").as[JsObject]
    lazy val playerAddTrackJson = (pathJson \ "/api/player/{pid}/playedTracks" \ "post").as[JsObject]
    lazy val resourceJson = (pathJson \ "/api/resource/").as[JsObject]
    lazy val allOptionalDefJson = (definitionsJson \ "com.iheart.playSwagger.AllOptional").as[JsObject]
    lazy val artistDefJson = (definitionsJson \ "com.iheart.playSwagger.Artist").as[JsObject]
    lazy val trackJson = (definitionsJson \ "com.iheart.playSwagger.Track").as[JsObject]
    lazy val studentJson = (definitionsJson \ "com.iheart.playSwagger.Student").asOpt[JsObject]
    lazy val teacherJson = (definitionsJson \ "com.iheart.playSwagger.Teacher").asOpt[JsObject]
    lazy val polymorphicContainerJson = (definitionsJson \ "com.iheart.playSwagger.PolymorphicContainer").asOpt[JsObject]
    lazy val polymorphicItemJson = (definitionsJson \ "com.iheart.playSwagger.PolymorphicItem").asOpt[JsObject]
    lazy val javaEnumContainerJson = (definitionsJson \ "com.iheart.playSwagger.JavaEnumContainer").asOpt[JsObject]
    lazy val overriddenDictTypeJson = (definitionsJson \ "com.iheart.playSwagger.DictType").as[JsObject]

    def parametersOf(json: JsValue): Seq[JsValue] = {
      (json \ "parameters").as[JsArray].value
    }

    "reads json comment" >> {
      (artistJson \ "summary").as[String] === "get recent tracks"
    }

    "reads optional field" >> {
      val limitParamJson = (artistJson \ "parameters").as[JsArray].value(1).as[JsObject]
      (limitParamJson \ "name").as[String] === "limit"
      (limitParamJson \ "format").as[String] === "int32"
      (limitParamJson \ "required").as[Boolean] === false
    }

    "merge comment in" >> {
      val sidPara = (stationJson \ "parameters").as[JsArray].value(0).as[JsObject]
      (sidPara \ "name").as[String] === "sid"
      (sidPara \ "description").as[String] === "station id"
      (sidPara \ "type").as[String] === "integer"
      (sidPara \ "required").as[Boolean] === true
    }

    "override generated with comment in" >> {
      val sidPara = (stationJson \ "parameters").as[JsArray].value(0).as[JsObject]
      (sidPara \ "format").as[String] === "int"
    }

    "not generate consumes in get" >> {
      (artistJson \ "consumes").toOption must beEmpty
    }

    "read definition from referenceTypes" >> {
      (trackJson \ "properties" \ "name" \ "type").as[String] === "string"
    }

    "read schema of referenced type" >> {
      (trackJson \ "properties" \ "artist" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Artist")
    }

    "read seq of referenced type" >> {
      val relatedProp = (trackJson \ "properties" \ "related")
      (relatedProp \ "type").asOpt[String] === Some("array")
      (relatedProp \ "items" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Artist")
    }

    "read seq of primitive type" >> {
      val numberProps = (trackJson \ "properties" \ "numbers")
      (numberProps \ "type").asOpt[String] === Some("array")
      (numberProps \ "items" \ "type").asOpt[String] === Some("integer")
    }

    "read definition from referenced referenceTypes" >> {

      (artistDefJson \ "properties" \ "age" \ "type").as[String] === "integer"
    }

    "read trait with container" >> {
      polymorphicContainerJson must beSome[JsObject]
      (polymorphicContainerJson.get \ "properties" \ "item" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.PolymorphicItem")
      polymorphicItemJson must beSome[JsObject]
    }

    "read java enum with container" >> {
      javaEnumContainerJson must beSome[JsObject]
      (javaEnumContainerJson.get \ "properties" \ "status" \ "enum").asOpt[Seq[String]] === Some(Seq("DISABLED", "ACTIVE"))
    }

    "definition property have no name" >> {
      (artistDefJson \ "properties" \ "age" \ "name").toOption must beEmpty
    }

    "generate post path with consumes" >> {
      (addTrackJson \ "consumes").as[JsArray].value(0).as[String] === "application/json"
    }

    "generate body parameter with in " >> {
      val params = parametersOf(addTrackJson)
      params.length === 1
      (params.head \ "in").asOpt[String] === Some("body")
      (params.head \ "schema" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Track")
    }

    "does not generate for end points marked as hidden" >> {
      (pathJson \ "/api/station/hidden" \ "get").toOption must beEmpty
    }

    "generate path correctly with missing type (String by default) in controller description" >> {
      (playerJson \ "summary").asOpt[String] === Some("get player")
    }

    "generate tags for an endpoint" >> {
      (playerJson \ "tags").asOpt[Seq[String]] === Some(Seq("player"))
    }

    "generate parameter for more path" >> {
      parametersOf(playerJson).length === 2
    }

    "generate tags definition" >> {
      val tags = (json \ "tags").asOpt[Seq[JsObject]]
      tags must beSome[Seq[JsObject]]
      tags.get.map(tO ⇒ (tO \ "name").as[String]).sorted must containAllOf(Seq("customResource", "liveMeta", "player", "resource"))
    }

    "merge tag description from base" >> {
      val tags = (json \ "tags").asOpt[Seq[JsObject]]
      tags must beSome[Seq[JsObject]]
      val playTag = tags.get.find(t ⇒ (t \ "name").as[String] == "player")
      (playTag.get \ "description").asOpt[String] === Some("this is player api")
    }

    "get both body and url params" >> {
      val params = (playerAddTrackJson \ "parameters").as[JsArray].value
      params.length === 2
      params.map(p ⇒ (p \ "name").as[String]).toSet === Set("body", "pid")

    }

    "get parameter type of" >> {
      val playerSearchJson = (pathJson \ "/api/player/{pid}/tracks/search" \ "get").as[JsObject]
      val params: Seq[JsValue] = parametersOf(playerSearchJson)
      params.length === 2

      "path params" >> {
        (params.head \ "name").as[String] === "pid"
        (params.head \ "in").as[String] === "path"
      }

      "query params" >> {
        (params.last \ "in").as[String] === "query"
      }

    }

    "allow multiple routes with the same path" >> {
      resourceJson.keys.toSet === Set("get", "post", "delete", "put")
    }

    "parse controller with custom namespace" >> {
      (pathJson \ "/api/customResource/" \ "get").asOpt[JsObject] must beSome[JsObject]
    }

    "parse class referenced in option type" >> {
      studentJson must beSome[JsObject]
      teacherJson must beSome[JsObject]
      (teacherJson.get \ "properties" \ "name" \ "type").as[String] === "string"
    }

    "parse param with default value as optional field" >> {
      val endPointJson = (pathJson \ "/api/students/defaultValueParam" \ "put").asOpt[JsObject]
      endPointJson must beSome[JsObject]

      val paramJson: JsValue = parametersOf(endPointJson.get).head

      (paramJson \ "name").as[String] === "aFlag"

      "set required as false" >> {
        (paramJson \ "required").as[Boolean] === false
      }

      "set in as query" >> {
        (paramJson \ "in").as[String] === "query"
      }

      "set default value" >> {
        (paramJson \ "default").as[Boolean] === true
      }
    }

    "should contain schemas in responses" >> {
      (postBodyJson \ "responses" \ "200" \ "schema" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.FooWithSeq2")
    }

    "should contain schemas in requests" >> {
      val paramJson = parametersOf(addTrackJson).head
      (paramJson \ "schema" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Track")
    }

    "excluded domain object should contain only spec from swagger.json" >> {
      overriddenDictTypeJson === Json.obj(
        "type" → "object",
        "properties" → Json.obj(
          "id" → Json.obj("type" → "string"),
          "value" → Json.obj("type" → "string")
        )
      )
    }

    "definition properties does not contain 'required' boolean field" >> {
      definitionsJson.as[JsObject].values.forall { definition ⇒
        (definition \ "properties").as[JsObject].values.forall { property ⇒
          (property \ "required").toOption === None
        }
      }
    }

    "definitions exposes 'required' array if there are required properties" >> {
      val requiredFields = Seq("name", "artist", "related", "numbers")
      (trackJson \ "required").as[Seq[String]] must contain(allOf(requiredFields: _*).exactly)
    }

    "definitions does not expose 'required' array if there are no required properties" >> {
      (allOptionalDefJson \ "required").asOpt[Seq[String]] === None
    }

    "handle multiple levels of includes" >> {
      val tags = (pathJson \ "/level1/level2/level3" \ "get" \ "tags").asOpt[Seq[String]]
      tags must beSome.which(_ == Seq("level2"))
    }

    "not contain tags that are empty" >> {
      val tags = (json \ "tags").as[Seq[JsObject]]
        .map(o ⇒ (o \ "name").as[String])
      tags must not contain "no"
    }

    "handle type aliases in post body" >> {
      val properties = (definitionsJson \ "com.iheart.playSwagger.FooWithSeq2" \ "properties").as[JsObject]
      (properties \ "abc1" \ "items" \ "type").as[String] === "string"
      (properties \ "abc2" \ "items" \ "items" \ "type").as[String] === "integer"
    }

    "accept parameter references" >> {
      val parameters = (pathJson \ "/references/magic/echoMagic/{type}" \ "post" \ "parameters").as[Seq[JsObject]]

      parameters must contain((entry: JsObject) ⇒
        entry.value.get("$ref").contains(JsString("#/parameters/magic")))
        .exactly(1.times)

      parameters must contain((entry: JsObject) ⇒
        entry.value.get("name").contains(JsString("notMagic")))
        .exactly(1.times)
    }

    // TODO: routes order

  }

}

