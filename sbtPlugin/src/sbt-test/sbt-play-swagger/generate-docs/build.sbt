
logLevel in update := sbt.Level.Warn

enablePlugins(PlayScala, SwaggerPlugin)

name := "app"

scalaVersion := "2.11.7"

swaggerDomainNameSpaces := Seq("namespace1", "namespace2")



TaskKey[Unit]("check") := {
  val expected =
    """
      |{
      |   "paths":{
      |      "/tracks/{trackId}":{
      |         "get":{
      |            "tags":[
      |               "routes"
      |            ],
      |            "summary":"Get the track metadata",
      |            "responses":{
      |               "200":{
      |                  "schema":{
      |                     "$ref":"#/definitions/namespace2.Track"
      |                  }
      |               }
      |            },
      |            "parameters":[
      |               {
      |                  "name":"path",
      |                  "type":"string",
      |                  "required":true,
      |                  "in":"query"
      |               },
      |               {
      |                  "name":"trackId",
      |                  "type":"asset",
      |                  "required":true,
      |                  "in":"path"
      |               }
      |            ]
      |         }
      |      }
      |   },
      |   "definitions":{
      |      "namespace1.Artist":{
      |         "properties":{
      |            "name":{
      |               "type":"string"
      |            },
      |            "age":{
      |               "type":"integer",
      |               "format":"int32"
      |            }
      |         },
      |         "required":[
      |            "name",
      |            "age"
      |         ]
      |      },
      |      "namespace2.Track":{
      |         "properties":{
      |            "name":{
      |               "type":"string"
      |            },
      |            "genre":{
      |               "type":"string"
      |            },
      |            "artist":{
      |               "$ref":"#/definitions/namespace1.Artist"
      |            },
      |            "related":{
      |               "type":"array",
      |               "items":{
      |                  "$ref":"#/definitions/namespace1.Artist"
      |               }
      |            },
      |            "numbers":{
      |               "type":"array",
      |               "items":{
      |                  "type":"integer",
      |                  "format":"int32"
      |               }
      |            }
      |         },
      |         "required":[
      |            "name",
      |            "artist",
      |            "related",
      |            "numbers"
      |         ]
      |      }
      |   },
      |   "swagger":"2.0",
      |   "info":{
      |      "title":"Poweramp API",
      |      "description":"My API is the best"
      |   },
      |   "tags":[
      |      {
      |         "name":"routes"
      |      }
      |   ]
      |}
    """.stripMargin.split('\n').map(_.trim.filter(_ >= ' ')).mkString

  val result = IO.read(target.value / "swagger" / "swagger.json")

  if (result != expected) {
    sys.error(
      s"""Swagger.json is off.
         |Result: $result
         |Expected: $expected
         |
       """.stripMargin)
  }
}
