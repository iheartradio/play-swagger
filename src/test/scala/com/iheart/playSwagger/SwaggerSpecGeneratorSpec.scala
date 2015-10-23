package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json.{ JsValue, Json, JsArray, JsObject }

case class Track(name: String, genre: Option[String], artist: Artist, related: Seq[Artist], numbers: Seq[Int])
case class Artist(name: String, age: Int)

case class Student(name: String, teacher: Option[Teacher])
case class Teacher(name: String)

class SwaggerSpecGeneratorSpec extends Specification {
  implicit val cl = getClass.getClassLoader
  "integration" >> {
    val routesDocumentation = Seq(
      ("GET", "/api/artist/$aid<[^/]+>/playedTracks/recent", "controllers.LiveMeta.playedByArtist(aid:Int, limit:Option[Int])"),

      ("GET", "/api/station/$sid<[^/]+>/playedTracks/last", "@controllers.LiveMeta@.playedByStation(sid:Int)"),
      ("POST", "/api/station/playedTracks", "controllers.LiveMeta.addPlayedTracks()"),
      ("GET", "/api/station/hidden", "controllers.LiveMeta.hiddenEndPoint()"),

      ("GET", "/api/player/$pid<.+>/context/$bid<.+>", "controllers.Player.getPlayer(pid:String, bid:String)"),
      ("GET", "/api/player/$pid<.+>/tracks/search", "controllers.Player.searchTrack(pid:String, bid:String)"),
      ("POST", "/api/player/$pid<.+>/playedTracks", "controllers.Player.addPlayedTracks(pid:String)"),

      ("GET", "/api/resource/", "controllers.Resource.get()"),
      ("PUT", "/api/resource/", "controllers.Resource.put()"),
      ("POST", "/api/resource/", "controllers.Resource.post()"),
      ("DELETE", "/api/resource/", "controllers.Resource.post()"),

      ("GET", "/api/customResource/", "com.iheart.controllers.Resource.get()"),

      ("GET", "/api/students/$name<.+>", "com.iheart.controllers.Students.get(name:String)")

    )
    val liveMetaRoutesLines =
      """
        |###
        |# {
        |#   "summary" : "get recent tracks",
        |#   "description" : " The Products endpoint returns information about the *Uber* products offered at a given location. The response includes the display name and other details about each product, and lists the products in the proper display order."
        |# }
        |###
        |
        |GET     /artist/:aid/playedTracks/recent           controllers.LiveMeta.playedByArtist(aid: Int, limit: Option[Int])
        |
        |###
        |#  summary: last track
        |#  description: big deal
        |#  parameters:
        |#    - name: sid
        |#      description: station id
        |#      format: int
        |#  responses:
        |#    200:
        |#      description: Profile information for a user
        |#      schema:
        |#        $ref: '#/definitions/com.iheart.playSwagger.Track'
        |###
        |GET     /station/:sid/playedTracks/last             @controllers.LiveMeta@.playedByStation(sid: Int)
        |
        |###
        |#  summary: Add track
        |#  parameters:
        |#    - name: body
        |#      description: track information
        |#      schema:
        |#        $ref: '#/definitions/com.iheart.playSwagger.Track'
        |#  responses:
        |#    200:
        |#      description: success
        |###
        |POST     /station/playedTracks             controllers.LiveMeta.addPlayedTracks()
        |
        |### NoDocs ###
        |GET      /station/hidden                   controllers.LiveMeta.hiddenEndPoint()
      """.stripMargin.split("\n").toList

    val playerRoutesLines =
      """
      |###
      |#  summary: get player
      |###
      |GET     /player/:pid/context/:bid                controllers.Player.getPlayer(pid, bid)
      |
      |GET     /player/:pid/tracks/search               controllers.Player.searchTrack(pid, keyword)
      |
      |###
      |#  parameters:
      |#    - name: body
      |#      description: track information
      |#      schema:
      |#        $ref: '#/definitions/com.iheart.playSwagger.Track'
      |###
      |POST     /player/:pid/playedTracks             controllers.Player.addPlayedTracks(pid)
      |
    """.stripMargin.split("\n").toList

    val resourceRoutesLines =
      """
      |GET     /api/resource/   controllers.Resource.get()
      |PUT     /api/resource/   controllers.Resource.put()
      |POST    /api/resource/   controllers.Resource.post()
      |DELETE  /api/resource/   controllers.Resource.delete()
    """.stripMargin.split("\n").toList

    val customControllerLines =
      """
      |GET     /api/customResource/    com.iheart.controllers.Resource.get()
    """.stripMargin.split("\n").toList

    val studentsLines =
    """
      |###
      |#  responses:
      |#    200:
      |#      schema:
      |#        $ref: '#/definitions/com.iheart.playSwagger.Student'
      |###
      |GET     /api/students/:name    com.iheart.controllers.Students.get(name)
    """.stripMargin.split("\n").toList

    val base = Json.parse(
      """
        |{
        |  "tags": [
        |    {
        |       "name" : "player",
        |       "description": "this is player api"
        |    }
        |  ]
        |}
      """.stripMargin
    ).asInstanceOf[JsObject]

    val routesLines = Map(
      "liveMeta" → liveMetaRoutesLines,
      "player" → playerRoutesLines,
      "resource" → resourceRoutesLines,
      "customResource" → customControllerLines,
      "student" → studentsLines)

    lazy val json = SwaggerSpecGenerator(Some("com.iheart")).generateWithBase(routesDocumentation, routesLines, base)
    lazy val pathJson = json \ "paths"
    lazy val definitionsJson = json \ "definitions"
    lazy val artistJson = (pathJson \ "/api/artist/{aid}/playedTracks/recent" \ "get").as[JsObject]
    lazy val stationJson = (pathJson \ "/api/station/{sid}/playedTracks/last" \ "get").as[JsObject]
    lazy val addTrackJson = (pathJson \ "/api/station/playedTracks" \ "post").as[JsObject]
    lazy val playerJson = (pathJson \ "/api/player/{pid}/context/{bid}" \ "get").as[JsObject]
    lazy val playerAddTrackJson = (pathJson \ "/api/player/{pid}/playedTracks" \ "post").as[JsObject]
    lazy val resourceJson = (pathJson \ "/api/resource/").as[JsObject]
    lazy val artistDefJson = (definitionsJson \ "com.iheart.playSwagger.Artist").as[JsObject]
    lazy val trackJson = (definitionsJson \ "com.iheart.playSwagger.Track").as[JsObject]
    lazy val studentJson = (definitionsJson \ "com.iheart.playSwagger.Student").asOpt[JsObject]
    lazy val teacherJson = (definitionsJson \ "com.iheart.playSwagger.Teacher").asOpt[JsObject]

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
      (trackJson \ "properties" \ "artist" \ "schema" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Artist")
    }

    "read seq of referenced type" >> {
      val relatedProp = (trackJson \ "properties" \ "related")
      (relatedProp \ "type").asOpt[String] === Some("array")
      (relatedProp \ "items" \ "$ref").asOpt[String] === Some("#/definitions/com.iheart.playSwagger.Artist")
    }

    "read seq of primitive type" >> {
      val numberProps = (trackJson \ "properties" \ "numbers")
      (numberProps \ "type").asOpt[String] === Some("array")
      (numberProps \ "items" \ "type").asOpt[String] === Some("Int")
    }

    "read definition from referenced referenceTypes" >> {

      (artistDefJson \ "properties" \ "age" \ "type").as[String] === "integer"
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
    }

    "does not generate for end points marked as hidden" >> {
      (pathJson \ "/api/station/hidden" \ "get").toOption must beEmpty
    }

    "generate path correctly with missing type (String by default) in controller description" >> {
      (playerJson \ "summary").asOpt[String] === Some("get player")
    }

    "generate tags for an end point" >> {
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
      (pathJson \ "/api/customResource/" \ "get").asOpt[JsObject] must beSome[JsObject]
    }

    "parse class referenced in option type" >> {
      studentJson must beSome[JsObject]
      teacherJson must beSome[JsObject]
      (teacherJson.get \ "properties" \ "name" \ "type" ).as[String] === "string"
    }
  }

}

