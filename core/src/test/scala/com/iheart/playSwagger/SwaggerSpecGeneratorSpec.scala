package com.iheart.playSwagger

import java.time.LocalDate

import com.iheart.playSwagger.RefinedTypes.{Age, Albums, SpotifyAccount}
import com.iheart.playSwagger.domain.CustomTypeMapping
import com.iheart.playSwagger.generator.SwaggerSpecGenerator
import org.specs2.mutable.Specification
import play.api.libs.json._

case class Track(name: String, genre: Option[String], artist: Artist, related: Seq[Artist], numbers: Seq[Int])
case class Artist(name: String, age: Age, spotifyAccount: SpotifyAccount, albums: Albums)

case class Student(name: String, teacher: Option[Teacher])
case class Teacher(name: String)

case class Animal(name: String, keeper: Keeper, birthDate: LocalDate, lastCheckup: Option[LocalDate])

case class Keeper(internalFieldName1: String, internalFieldName2: Int)

case class Subject(name: String)

/**
  * @param name e.g. Sunday, Monday, TuesDay...
  */
case class DayOfWeek(name: Option[String])

case class PolymorphicContainer(item: PolymorphicItem)
trait PolymorphicItem

case class EnumContainer(
    javaEnum: SampleJavaEnum,
    scalaEnum: SampleScalaEnum.SampleScalaEnum,
    enumeratumEnum: SampleEnumeratumEnum,
    enumeratumValueEnum: SampleEnumeratumValueEnum
)

case class AllOptional(a: Option[String], b: Option[String])

trait Parent
case class Child(name: String) extends Parent

case class EitherRepr[T](payload: Option[T], reason: Option[String])

case class Dog(legs: Int)
case class Cat(catName: String)

case class TypeParametricWrapper[T, O, S](simplePayload: T, maybeOtherPayload: Option[O], seq: Seq[S])

class SwaggerSpecGeneratorSpec extends Specification {
  implicit val cl: ClassLoader = getClass.getClassLoader
  val gen: SwaggerSpecGenerator = SwaggerSpecGenerator()

  "full path" >> {
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

    "respect trailing slash from previous element when in route path is root /" >> {
      gen.fullPath("p/", "/") === "/p/"
    }

    "respect trailing slash from previous element when in route path is empty" >> {
      gen.fullPath("p/", "") === "/p/"
    }

    "remove top level trailing slash" >> {
      gen.fullPath("p", "/") === "/p"
    }

  }

  "getCfgFile" >> {
    "valid swagger-custom-mappings yml" >> {
      val result = gen.readCfgFile[Seq[CustomTypeMapping]]("swagger-custom-mappings.yml")
      result must beSome[Seq[CustomTypeMapping]]
      val mappings = result.get
      mappings.size must be_>(2)
      mappings.head.`type` mustEqual "java\\.time\\.LocalDate"
      mappings.head.specAsParameter === List(Json.obj("type" -> "string", "format" -> "date"))
      mappings.head.specAsProperty must beEmpty

    }

    "invalid swagger-settings yml" >> {
      gen.readCfgFile[Seq[CustomTypeMapping]]("swagger-custom-mappings_invalid.yml") must throwA[JsResultException]
    }
  }

}

class SwaggerSpecGeneratorIntegrationSpec extends Specification {
  implicit val cl: ClassLoader = getClass.getClassLoader

  "integration" >> {

    lazy val defaultRoutesFile = SwaggerSpecGenerator(modelQualifier = ExcludingDomainQualifier).generate()

    "Use default routes file when no argument is given" >> {
      val json = defaultRoutesFile.get
      (json \ "paths" \ "/player/{pid}/context/{bid}").asOpt[JsObject] must beSome
    }
    "Default routes tag must not be present" >> {
      val json = defaultRoutesFile.get
      (json \ "tags").asOpt[JsObject] must beNone
    }

    lazy val json = SwaggerSpecGenerator(false, false, false, "com.iheart").generate("test.routes").get
    lazy val pathJson = json \ "paths"
    lazy val definitionsJson = json \ "definitions"
    lazy val postBodyJson = (pathJson \ "/post-body" \ "post").as[JsObject]
    lazy val artistJson = (pathJson \ "/api/artist/{aid}/playedTracks/recent" \ "get").as[JsObject]
    lazy val stationJson = (pathJson \ "/api/station/{sid}/playedTracks/last" \ "get").as[JsObject]
    lazy val addTrackJson = (pathJson \ "/api/station/playedTracks" \ "post").as[JsObject]
    lazy val playerJson = (pathJson \ "/api/player/{pid}/context/{bid}" \ "get").as[JsObject]
    lazy val playerAddTrackJson = (pathJson \ "/api/player/{pid}/playedTracks" \ "post").as[JsObject]
    lazy val resourceJson = (pathJson \ "/api/resource").as[JsObject]
    lazy val allOptionalDefJson = (definitionsJson \ "com.iheart.playSwagger.AllOptional").as[JsObject]
    lazy val artistDefJson = (definitionsJson \ "com.iheart.playSwagger.Artist").as[JsObject]
    lazy val trackJson = (definitionsJson \ "com.iheart.playSwagger.Track").as[JsObject]
    lazy val studentJson = (definitionsJson \ "com.iheart.playSwagger.Student").asOpt[JsObject]
    lazy val teacherJson = (definitionsJson \ "com.iheart.playSwagger.Teacher").asOpt[JsObject]
    lazy val dayOfWeekJson = (definitionsJson \ "com.iheart.playSwagger.DayOfWeek").asOpt[JsObject]
    lazy val polymorphicContainerJson =
      (definitionsJson \ "com.iheart.playSwagger.PolymorphicContainer").asOpt[JsObject]
    lazy val polymorphicItemJson = (definitionsJson \ "com.iheart.playSwagger.PolymorphicItem").asOpt[JsObject]
    lazy val enumContainerJson = (definitionsJson \ "com.iheart.playSwagger.EnumContainer").asOpt[JsObject]
    lazy val overriddenDictTypeJson = (definitionsJson \ "com.iheart.playSwagger.DictType").as[JsObject]
    lazy val typeParametricWrapperJson =
      (definitionsJson \ "com.iheart.playSwagger.TypeParametricWrapper[String, com.iheart.playSwagger.EitherRepr[com.iheart.playSwagger.Cat], Seq[com.iheart.playSwagger.EitherRepr[com.iheart.playSwagger.Dog]]]").as[
        JsObject
      ]
    lazy val otherTypeParametricWrapperJson =
      (definitionsJson \ "com.iheart.playSwagger.TypeParametricWrapper[Option[String], Seq[com.iheart.playSwagger.Cat], Seq[Option[com.iheart.playSwagger.Dog]]]").as[
        JsObject
      ]
    lazy val dogEitherJson =
      (definitionsJson \ "com.iheart.playSwagger.EitherRepr[com.iheart.playSwagger.Dog]").as[JsObject]
    lazy val catEitherJson =
      (definitionsJson \ "com.iheart.playSwagger.EitherRepr[com.iheart.playSwagger.Cat]").as[JsObject]
    lazy val dogJson = (definitionsJson \ "com.iheart.playSwagger.Dog").as[JsObject]
    lazy val catJson = (definitionsJson \ "com.iheart.playSwagger.Cat").as[JsObject]

    def parametersOf(json: JsValue): List[JsValue] = {
      (json \ "parameters").as[JsArray].value.toList
    }

    "reads json comment" >> {
      (artistJson \ "summary").as[String] === "get recent tracks"
    }

    "reads optional field" >> {
      val limitParamJson = (artistJson \ "parameters").as[JsArray].value(1).as[JsObject]
      (limitParamJson \ "name").as[String] === "limit"
      (limitParamJson \ "format").as[String] === "int32"
      (limitParamJson \ "default").asOpt[String] === None
      (limitParamJson \ "required").as[Boolean] === false
      (limitParamJson \ "x-nullable").as[Boolean] === true
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
      (trackJson \ "properties" \ "artist" \ "$ref").asOpt[String] === Some(
        "#/definitions/com.iheart.playSwagger.Artist"
      )
    }

    "read seq of referenced type" >> {
      val relatedProp = trackJson \ "properties" \ "related"
      (relatedProp \ "type").asOpt[String] === Some("array")
      (relatedProp \ "items" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Artist")
    }

    "read seq of primitive type" >> {
      val numberProps = trackJson \ "properties" \ "numbers"
      (numberProps \ "type").asOpt[String] === Some("array")
      (numberProps \ "items" \ "type").asOpt[String] === Some("integer")
    }

    "read definition from referenced referenceTypes" >> {

      (artistDefJson \ "properties" \ "age" \ "type").as[String] === "integer"
      (artistDefJson \ "properties" \ "spotifyAccount" \ "type").as[String] === "string"
      (artistDefJson \ "properties" \ "albums" \ "type").as[String] === "array"
      (artistDefJson \ "properties" \ "albums" \ "items" \ "type").as[String] === "string"
    }

    "read trait with container" >> {
      polymorphicContainerJson must beSome[JsObject]
      (polymorphicContainerJson.get \ "properties" \ "item" \ "$ref").asOpt[String] === Some(
        "#/definitions/com.iheart.playSwagger.PolymorphicItem"
      )
      polymorphicItemJson must beSome[JsObject]
    }

    "read java enum with container" >> {
      enumContainerJson must beSome[JsObject]
      (enumContainerJson.get \ "properties" \ "javaEnum" \ "enum").asOpt[Seq[String]] === Some(Seq(
        "DISABLED",
        "ACTIVE"
      ))
    }

    "read scala enum with container" >> {
      enumContainerJson must beSome[JsObject]
      (enumContainerJson.get \ "properties" \ "scalaEnum" \ "enum").asOpt[Seq[String]] === Some(Seq("One", "Two"))
    }

    "read enumeratum enum with container" >> {
      enumContainerJson must beSome[JsObject]
      (enumContainerJson.get \ "properties" \ "enumeratumEnum" \ "enum").asOpt[Seq[String]] === Some(Seq(
        "info_one",
        "info_two"
      ))
    }

    "read enumeratum value enum with container" >> {
      enumContainerJson must beSome[JsObject]
      (enumContainerJson.get \ "properties" \ "enumeratumValueEnum" \ "enum").asOpt[Seq[String]] === Some(Seq(
        "valueOne",
        "valueTwo"
      ))
    }

    "read parametric type wrappers" >> {
      (typeParametricWrapperJson \ "properties" \ "simplePayload" \ "type").as[String] === "string"
      (typeParametricWrapperJson \ "properties" \ "maybeOtherPayload" \ "$ref").as[
        String
      ] === "#/definitions/com.iheart.playSwagger.EitherRepr[com.iheart.playSwagger.Cat]"
      (typeParametricWrapperJson \ "properties" \ "seq" \ "items" \ "items" \ "$ref").as[String] === "#/definitions/com.iheart.playSwagger.EitherRepr[com.iheart.playSwagger.Dog]"

      (typeParametricWrapperJson \ "required").as[Seq[String]] === Seq("simplePayload", "seq")

      (otherTypeParametricWrapperJson \ "properties" \ "simplePayload" \ "type").as[String] === "string"
      (otherTypeParametricWrapperJson \ "properties" \ "maybeOtherPayload" \ "type").as[String] === "array"
      (otherTypeParametricWrapperJson \ "properties" \ "maybeOtherPayload" \ "items" \ "$ref").as[String] === "#/definitions/com.iheart.playSwagger.Cat"
      (otherTypeParametricWrapperJson \ "properties" \ "seq" \ "type").as[String] === "array"
      (otherTypeParametricWrapperJson \ "properties" \ "seq" \ "items" \ "type").as[String] === "array"
      (otherTypeParametricWrapperJson \ "properties" \ "seq" \ "items" \ "items" \ "$ref").as[String] === "#/definitions/com.iheart.playSwagger.Dog"
      (otherTypeParametricWrapperJson \ "required").as[Seq[String]] === Seq("seq")

      (dogEitherJson \ "properties" \ "payload" \ "$ref").as[String] === "#/definitions/com.iheart.playSwagger.Dog"
      (catEitherJson \ "properties" \ "payload" \ "$ref").as[String] === "#/definitions/com.iheart.playSwagger.Cat"
      (dogJson \ "properties" \ "legs" \ "type").as[String] === "integer"
      (catJson \ "properties" \ "catName" \ "type").as[String] === "string"
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

    "generate body parameter without in if already provided" >> {
      val params = parametersOf((pathJson \ "/iWantAQueryBody" \ "post").as[JsObject])
      params.length === 1
      (params.head \ "in").asOpt[String] === Some("query")
    }

    "does not generate for end points marked as hidden" >> {
      "single" >> {
        (pathJson \ "/api/station/hidden" \ "get").toOption must beEmpty
      }
      "can batch skip within a file" >> {
        "multiple-a" >> {
          (pathJson \ "/api/station/hidden/a" \ "get").toOption must beEmpty
        }
        "multiple-b" >> {
          (pathJson \ "/api/station/hidden/b" \ "get").toOption must beEmpty
        }
        "multiple-c" >> {
          (pathJson \ "/api/station/hidden/c" \ "get").toOption must beEmpty
        }
      }
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
      tags.get.map(tO => (tO \ "name").as[String]).sorted must contain(exactly("player"))

      val allTags =
        pathJson.as[JsObject].value.values.flatMap { pathObj =>
          pathObj.as[JsObject].value.values.flatMap { opObj =>
            val tag = (opObj \ "tags").asOpt[Seq[String]]

            tag must beSome[Seq[String]]
            tag.get must have size 1
            tag.get
          }
        }.toSet

      allTags must containAllOf(Seq(
        "test",
        "students",
        "player",
        "resource",
        "level2",
        "customResource",
        "liveMeta",
        "referencing",
        "zoo"
      ))
    }

    "merge tag description from base" >> {
      val tags = (json \ "tags").asOpt[Seq[JsObject]]
      tags must beSome[Seq[JsObject]]
      val playTag = tags.get.find(t => (t \ "name").as[String] == "player")
      (playTag.get \ "description").asOpt[String] === Some("this is player api")
    }

    "get both body and url params" >> {
      val params = (playerAddTrackJson \ "parameters").as[JsArray].value
      params.length === 2
      params.map(p => (p \ "name").as[String]).toSet === Set("body", "pid")

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
      (pathJson \ "/api/customResource" \ "get").asOpt[JsObject] must beSome[JsObject]
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

      "not set nullable" >> {
        (paramJson \ "x-nullable").isEmpty
      }
    }

    "parse param with default triple quoted string value as optional field" >> {
      val endPointJson = (pathJson \ "/api/students/defaultValueParamString3" \ "put").asOpt[JsObject]
      endPointJson must beSome[JsObject]

      val paramJson: JsValue = parametersOf(endPointJson.get).head

      (paramJson \ "name").as[String] === "strFlag"

      "set required as false" >> {
        (paramJson \ "required").as[Boolean] === false
      }

      "set in as query" >> {
        (paramJson \ "in").as[String] === "query"
      }

      "set default value" >> {
        (paramJson \ "default").as[String] === """defaultValue with triple quotes"""
      }

      "not set nullable" >> {
        (paramJson \ "x-nullable").isEmpty
      }
    }

    "parse param with default string value as optional field" >> {
      val endPointJson = (pathJson \ "/api/students/defaultValueParamString" \ "put").asOpt[JsObject]
      endPointJson must beSome[JsObject]

      val paramJson: JsValue = parametersOf(endPointJson.get).head

      (paramJson \ "name").as[String] === "strFlag"

      "set required as false" >> {
        (paramJson \ "required").as[Boolean] === false
      }

      "set in as query" >> {
        (paramJson \ "in").as[String] === "query"
      }

      "set default value" >> {

        (paramJson \ "default").as[String] === "defaultValue"
      }

      "not set nullable" >> {
        (paramJson \ "x-nullable").isEmpty
      }
    }

    "parse param with default None value as optional string field" >> {
      val endPointJson = (pathJson \ "/api/students/defaultValueParamOptionalString" \ "put").asOpt[JsObject]
      endPointJson must beSome[JsObject]

      val paramJson: JsValue = parametersOf(endPointJson.get).head

      (paramJson \ "name").as[String] === "optionFlag"

      "set required as false" >> {
        (paramJson \ "required").as[Boolean] === false
      }

      "set in as query" >> {
        (paramJson \ "in").as[String] === "query"
      }

      "set default value" >> {
        (paramJson \ "default").asOpt[String] === None
      }

      "not set nullable" >> {
        (paramJson \ "x-nullable").as[Boolean]
      }
    }

    "parse param with default None value as optional integer field" >> {
      val endPointJson = (pathJson \ "/api/students/defaultValueParamOptionalInteger" \ "put").asOpt[JsObject]
      endPointJson must beSome[JsObject]

      val paramJson: JsValue = parametersOf(endPointJson.get).head

      (paramJson \ "name").as[String] === "optionFlag"

      "set required as false" >> {
        (paramJson \ "required").as[Boolean] === false
      }

      "set in as query" >> {
        (paramJson \ "in").as[String] === "query"
      }

      "set default value" >> {
        (paramJson \ "default").asOpt[String] === None
      }

      "not set nullable" >> {
        (paramJson \ "x-nullable").as[Boolean]
      }
    }

    "parse class referenced in referenced external file" >> {
      dayOfWeekJson must beSome[JsObject]
      (dayOfWeekJson.get \ "properties" \ "name" \ "type").as[String] === "string"
    }

    "embedded scaladoc strings" >> {
      lazy val json = SwaggerSpecGenerator(false, false, embedScaladoc = true, "com.iheart").generate("test.routes").get
      lazy val definitionsJson = json \ "definitions"
      lazy val dayOfWeekJson = (definitionsJson \ "com.iheart.playSwagger.DayOfWeek").asOpt[JsObject]
      dayOfWeekJson must beSome[JsObject]
      (dayOfWeekJson.get \ "properties" \ "name" \ "description").as[String] === "e.g. Sunday, Monday, TuesDay..."
    }

    "don't embedded scaladoc strings" >> {
      dayOfWeekJson must beSome[JsObject]
      (dayOfWeekJson.get \ "properties" \ "name" \ "description").asOpt[String] === None
    }

    "parse mixin referenced external file" >> {
      lazy val subjectJson = (pathJson \ "/api/subjects/dow/{subject}" \ "get").as[JsObject]

      "parse param" >> {
        val properties = (definitionsJson \ "com.iheart.playSwagger.Subject" \ "properties").as[JsObject]
        (properties \ "name" \ "type").as[String] === "string"
      }

      "embedding mixed responses" >> {
        "description" >> {
          (subjectJson \ "responses" \ "200" \ "description").as[String] === "success"
        }
        "reference schema" >> {
          (subjectJson \ "responses" \ "200" \ "content" \ "application/json" \ "schema" \ "$ref").as[String] ===
            "#/definitions/com.iheart.playSwagger.DayOfWeek"
        }
      }
    }

    "Top-level $ref is also embedded" >> {
      lazy val subjectNotJson = (pathJson \ "/api/subjects/dow/not/{subject}" \ "get").as[JsObject]

      "Information written in comments is mixed with external files" >> {
        (subjectNotJson \ "description").as[String] === "What day of the week is that subject?"
      }
      "description_200" >> {
        (subjectNotJson \ "responses" \ "200" \ "description").as[String] === "success"
      }
      "description_400" >> {
        (subjectNotJson \ "responses" \ "400" \ "description").as[String] === "error"
      }
      "reference schema" >> {
        (subjectNotJson \ "responses" \ "200" \ "content" \ "application/json" \ "schema" \ "$ref").as[String] ===
          "#/definitions/com.iheart.playSwagger.DayOfWeek"
      }
    }

    "should contain schemas in responses" >> {
      (postBodyJson \ "responses" \ "200" \ "schema" \ "$ref").asOpt[String] === Some(
        "#/definitions/com.iheart.playSwagger.FooWithSeq2"
      )
    }

    "should contain schemas in requests" >> {
      val paramJson = parametersOf(addTrackJson).head
      (paramJson \ "schema" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Track")
    }

    "excluded domain object should contain only spec from swagger.json" >> {
      overriddenDictTypeJson === Json.obj(
        "type" -> "object",
        "properties" -> Json.obj(
          "id" -> Json.obj("type" -> "string"),
          "value" -> Json.obj("type" -> "string")
        )
      )
    }

    "definition properties does not contain 'required' boolean field" >> {
      definitionsJson.as[JsObject].values.forall { definition =>
        (definition \ "properties").as[JsObject].values.forall { property =>
          (property \ "required").toOption === None
        }
      }
    }

    "definitions exposes 'required' array if there are required properties" >> {
      val requiredFields = Seq("name", "artist", "related", "numbers")
      (trackJson \ "required").as[Seq[String]] must contain(allOf(requiredFields.toSeq: _*).exactly)
    }

    "definitions does not expose 'required' array if there are no required properties" >> {
      (allOptionalDefJson \ "required").asOpt[Seq[String]] === None
    }

    "properties set x-nullable on options" >> {
      (trackJson \ "properties" \ "genre" \ "x-nullable").as[Boolean] === true
    }

    "properties don't set x-nullable on non-options" >> {
      (trackJson \ "properties" \ "name" \ "x-nullable").isEmpty === true
    }

    "handle multiple levels of includes" >> {
      val tags = (pathJson \ "/level1/level2/level3" \ "get" \ "tags").asOpt[Seq[String]]
      tags must beSome.which(_ == Seq("level2"))
    }

    "hornor the trailing slash" >> {
      val tags = (pathJson \ "/level1/level2/" \ "get" \ "tags").asOpt[Seq[String]]
      tags must beSome.which(_ == Seq("level2"))
    }

    "not contain tags that are empty" >> {
      val tags = (json \ "tags").as[Seq[JsObject]]
        .map(o => (o \ "name").as[String])
      tags must not contain "no"
    }

    "handle type aliases in post body" >> {
      val properties = (definitionsJson \ "com.iheart.playSwagger.FooWithSeq2" \ "properties").as[JsObject]
      (properties \ "abc1" \ "items" \ "type").as[String] === "string"
      (properties \ "abc2" \ "items" \ "items" \ "type").as[String] === "integer"
    }

    "accept parameter references" >> {
      val parameters = (pathJson \ "/references/magic/echoMagic/{type}" \ "post" \ "parameters").as[Seq[JsObject]]

      parameters must contain((entry: JsObject) =>
        entry.value.get("$ref").contains(JsString("#/parameters/magic"))
      )
        .exactly(1.times)

      parameters must contain((entry: JsObject) =>
        entry.value.get("name").contains(JsString("notMagic"))
      )
        .exactly(1.times)
    }

    "hide parameter set to ignore in the custom type mappings" >> {

      val parameters = (pathJson \ "/zoo/zone/{zid}/animals/{aid}" \ "get" \ "parameters").as[Seq[JsObject]]
      parameters.size === 1
      parameters must contain((entry: JsObject) =>
        entry.value.get("name").contains(JsString("aid"))
      )
        .exactly(1.times).not

    }

    "map parameter set according to the custom type mappings" >> {

      val parameters = (pathJson \ "/zoo/zone/{zid}/animals/{aid}" \ "get" \ "parameters").as[Seq[JsObject]]
      parameters.size === 1
      parameters.head.value.get("name") === Some(JsString("zid"))
      parameters.head.value.get("type") === Some(JsString("string"))
      parameters.head.value.get("required") === Some(JsBoolean(true))
    }

    "generate definition reference according to custom type mappings" >> {

      val properties = (definitionsJson \ "com.iheart.playSwagger.Animal" \ "properties").as[JsObject]
      (properties \ "keeper" \ "$ref").as[String] === "#/definitions/Keeper"

      val birthDateProperties = (properties \ "birthDate").get
      (birthDateProperties \ "type").as[String] === "string"
      (birthDateProperties \ "format").as[String] === "date"
      (birthDateProperties \ "x-nullable").isEmpty === true

      val lastCheckupProperties = (properties \ "lastCheckup").get
      (lastCheckupProperties \ "x-nullable").as[Boolean] === true

      (definitionsJson \ "com.iheart.playSwagger.Keeper").toOption must beEmpty
    }

    "custom type mappings in definition should be included in required" >> {

      val required = (definitionsJson \ "com.iheart.playSwagger.Animal" \ "required").as[JsArray]

      required.value must contain(JsString("keeper"))
    }

    "add operation id" >> {
      (addTrackJson \ "operationId").as[String] ==== "addPlayedTracks"
    }

    "fully operation id" >> {
      lazy val json = SwaggerSpecGenerator(false, true, false, "com.iheart").generate("test.routes").get
      lazy val addTrackJson = (json \ "paths" \ "/api/station/playedTracks" \ "post").as[JsObject]
      (addTrackJson \ "operationId").as[String] ==== "LiveMeta.addPlayedTracks"
    }

    "should maintain route file order" >> {
      // use students routes

      val pathJsonObj = pathJson.as[JsObject]
      val fieldKeys: List[String] = pathJsonObj.fields.map(_._1).toList
      val fieldsWithIndexMap = fieldKeys.zipWithIndex.toMap

      val first = fieldsWithIndexMap.get("/api/students/{name}")
      val second = fieldsWithIndexMap.get("/api/students/defaultValueParam")
      val third = fieldsWithIndexMap.get("/api/students/defaultValueParamString")
      val fourth = fieldsWithIndexMap.get("/api/students/defaultValueParamString3")

      // all paths must be retrieved
      first must beSome[Int]
      second must beSome[Int]
      third must beSome[Int]
      fourth must beSome[Int]

      // must be in exact order with nothing in between
      second.get - first.get === 1
      third.get - second.get === 1
      fourth.get - third.get === 1
    }

    "should retain $refs in 'swagger-custom-mappings'" >> {
      (definitionsJson \ "com.iheart.playSwagger.Child").toOption.isDefined === true
    }
  }

  "integration v3" >> {
    lazy val json = SwaggerSpecGenerator(true, false, false, "com.iheart").generate("testV3.routes").get
    lazy val componentSchemasJson = json \ "components" \ "schemas"
    lazy val trackJson = (componentSchemasJson \ "com.iheart.playSwagger.Track").as[JsObject]

    "read definition from referenceTypes" >> {
      (trackJson \ "properties" \ "name" \ "type").as[String] === "string"
    }

    "param data type values are in the correct location" >> {
      val contextParams = json \ "paths" \ "/{pid}/context/{bid}" \ "get" \ "parameters"
      val firstParam = contextParams \ 0
      (firstParam \ "schema" \ "type").as[String] === "string"
      (firstParam \ "required").as[Boolean] === true
    }

    "custom param data type values are in the correct location" >> {
      val parameters = (json \ "paths" \ "/zoo/zone/{zid}/animals/{aid}" \ "get" \ "parameters").as[Seq[JsObject]]
      parameters.size === 1

      val firstParam = parameters.head
      (firstParam \ "name").as[String] === "zid"
      (firstParam \ "schema" \ "type").as[String] === "string"
      (firstParam \ "required").as[Boolean] === true
    }

    "properties set nullable on options" >> {
      (trackJson \ "properties" \ "genre" \ "nullable").as[Boolean] === true
    }

    "properties don't set nullable on non-options" >> {
      (trackJson \ "properties" \ "name" \ "nullable").isEmpty === true
    }
  }
}
