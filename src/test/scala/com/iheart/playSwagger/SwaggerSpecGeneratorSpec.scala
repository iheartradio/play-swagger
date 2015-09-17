package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json.{Json, JsArray, JsObject}

case class Track(name: String, genre: Option[String], artist: Artist)
case class Artist(name: String, age: Int)

class SwaggerSpecGeneratorSpec  extends Specification {
  implicit val cl = getClass.getClassLoader
  "integration" >> {
    val routesDocumentation = Seq(
      ("GET","/api/artist/$aid<[^/]+>/playedTracks/recent","controllers.LiveMeta.playedByArtist(aid:Int, limit:Option[Int])"),
      ("GET","/api/station/$sid<[^/]+>/playedTracks/last", "@controllers.LiveMeta@.playedByStation(sid:Int)"),
      ("POST","/api/station/playedTracks", "controllers.LiveMeta.addPlayedTracks()"),
      ("GET","/api/player/$pid<.+>/context/$bid<.+>", "controllers.Player.getPlayer(pid:String, bid:String)"),
      ("POST","/api/player/$pid<.+>/playedTracks", "controllers.Player.addPlayedTracks(pid:String)"),
      ("GET","/api/station/hidden", "controllers.LiveMeta.hiddenEndPoint()")
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
      |GET     /player/:pid/context/:bid                             controllers.Player.getPlayer(pid, bid)
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
      """.stripMargin).asInstanceOf[JsObject]

    val routesLines = Map("liveMeta" → liveMetaRoutesLines, "player" → playerRoutesLines)


    val json =  SwaggerSpecGenerator(Some("com.iheart")).generateWithBase(routesDocumentation, routesLines, base)
    val pathJson = json \ "paths"
    val definitionsJson = json \ "definitions"
    val artistJson = (pathJson \ "/api/artist/{aid}/playedTracks/recent" \ "get").as[JsObject]
    val stationJson = (pathJson \ "/api/station/{sid}/playedTracks/last" \ "get").as[JsObject]
    val addTrackJson = (pathJson \ "/api/station/playedTracks" \ "post").as[JsObject]
    val playerJson = (pathJson \ "/api/player/{pid}/context/{bid}" \ "get").as[JsObject]
    val playerAddTrackJson = (pathJson \ "/api/player/{pid}/playedTracks" \ "post").as[JsObject]
    val artistDefJson = (definitionsJson \ "com.iheart.playSwagger.Artist").as[JsObject]

    "reads json comment" >> {
      (artistJson \ "summary" ).as[String] === "get recent tracks"
    }

    "reads optional field" >> {
      val limitParamJson = (artistJson \ "parameters" ).as[JsArray].value(1).as[JsObject]
      (limitParamJson \ "name").as[String] === "limit"
      (limitParamJson \ "format").as[String] === "int32"
      (limitParamJson \ "required").as[Boolean] === false
    }

    "merge comment in" >> {
      val sidPara = (stationJson \ "parameters" ).as[JsArray].value(0).as[JsObject]
      (sidPara \ "name").as[String] === "sid"
      (sidPara \ "description").as[String] === "station id"
      (sidPara \ "type").as[String] === "integer"
      (sidPara \ "required").as[Boolean] === true
    }

    "override generated with comment in" >> {
      val sidPara = (stationJson \ "parameters" ).as[JsArray].value(0).as[JsObject]
      (sidPara \ "format").as[String] === "int"
    }

    "not generate consumes in get" >> {
      (artistJson \ "consumes" ).toOption must beEmpty
    }

    "read definition from referenceTypes" >> {
      val trackJson = (definitionsJson \ "com.iheart.playSwagger.Track").as[JsObject]
      (trackJson \ "properties" \ "name" \ "type" ).as[String] === "string"
    }

    "read definition from referenced referenceTypes" >> {

      (artistDefJson \ "properties" \ "age" \ "type" ).as[String] === "integer"
    }

    "definition property have no name" >> {
      (artistDefJson \ "properties" \ "age" \ "name" ).toOption must beEmpty
    }

    "generate post path with consumes" >> {
      (addTrackJson \ "consumes" ).as[JsArray].value(0).as[String] === "application/json"
    }

    "generate body parameter with paramType " >> {
      (addTrackJson \ "parameters" ).as[JsArray].value.length === 1
      ((addTrackJson \ "parameters" ).as[JsArray].value(0) \ "paramType").asOpt[String] === Some("body")
    }

    "does not generate for end points marked as hidden" >> {
      (pathJson \ "/api/station/hidden" \ "get").toOption must beEmpty
    }

    "generate path correctly with missing type (String by default) in controller descrption" >> {
      (playerJson \ "summary").asOpt[String] === Some("get player")
    }

    "generate tags for an end point" >> {
      (playerJson \ "tags").asOpt[Seq[String]] === Some(Seq("player"))
    }

    "generate parameter for more path" >> {
      (playerJson \ "parameters").as[JsArray].value.length === 2
    }

    "generate tags definition" >> {
      val tags = (json \ "tags").asOpt[Seq[JsObject]]
      tags must beSome[Seq[JsObject]]
      tags.get.map( tO ⇒  (tO \ "name").as[String]).sorted === Seq("liveMeta", "player").sorted
    }

    "merge tag description from base" >> {
      val tags = (json \ "tags").asOpt[Seq[JsObject]]
      tags must beSome[Seq[JsObject]]
      val playTag = tags.get.find( t ⇒ (t \ "name").as[String] == "player")
      (playTag.get \ "description").asOpt[String] ===  Some("this is player api")
    }

    "get both body and url params" >> {
      val params = (playerAddTrackJson \ "parameters").as[JsArray].value
      params.length === 2
      params.map(p => (p \ "name").as[String]).toSet === Set("body", "pid")

    }

  }

}
