import spray.json._
import DefaultJsonProtocol._

update / logLevel := sbt.Level.Warn

enablePlugins(PlayScala, SwaggerPlugin)

name := "app"

version := "1.0.1-BETA1"

scalaVersion := "2.12.18"

swaggerDomainNameSpaces := Seq("namespace1", "namespace2")

swaggerRoutesFile := "my-routes"

swaggerOutputTransformers := Seq(envOutputTransformer)

swaggerPlayJava := false

val pathVal = System.getenv("PATH")

TaskKey[Unit]("check") := {

  def uniform(jsString: String): String = jsString.parseJson.prettyPrint

  val expected = uniform(
    s"""
      |{
      |   "paths":{
      |      "/tracks/{trackId}":{
      |         "get":{
      |           "operationId":"versioned",
      |            "tags":[
      |               "${swaggerRoutesFile.value}"
      |            ],
      |            "summary":"Get the track metadata",
      |            "responses":{
      |               "200":{
      |                  "summary": "${pathVal.replace("\\", "\\\\")}",
      |                  "schema":{
      |                     "$$ref":"#/definitions/namespace2.Track"
      |                  }
      |               }
      |            },
      |            "parameters":[
      |               {
      |                  "in":"path",
      |                  "name":"trackId",
      |                  "type":"asset",
      |                  "required":true
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
      |            },
      |            "birthdate":{
      |               "type":"string",
      |               "format":"date"
      |            }
      |         },
      |         "required":[
      |            "name",
      |            "age",
      |            "birthdate"
      |         ]
      |      },
      |      "namespace2.Track":{
      |         "properties":{
      |            "name":{
      |               "type":"string"
      |            },
      |            "genre":{
      |               "type":"string",
      |               "x-nullable": true
      |            },
      |            "artist":{
      |               "$$ref":"#/definitions/namespace1.Artist"
      |            },
      |            "related":{
      |               "type":"array",
      |               "items":{
      |                  "$$ref":"#/definitions/namespace1.Artist"
      |               }
      |            },
      |            "numbers":{
      |               "type":"array",
      |               "items":{
      |                  "type":"integer",
      |                  "format":"int32"
      |               }
      |            },
      |            "length":{
      |               "type":"integer"
      |            }
      |         },
      |         "required":[
      |            "name",
      |            "artist",
      |            "related",
      |            "numbers",
      |            "length"
      |         ]
      |      }
      |   },
      |   "swagger":"2.0",
      |   "info":{
      |      "version":"1.0.1-BETA1",
      |      "title":"Poweramp API",
      |      "description":"My API is the best"
      |   },
      |   "tags":[]
      |}
    """.stripMargin
  )

  val result = uniform(IO.read(swaggerTarget.value / swaggerFileName.value))

  if (result != expected) {
    val rs = result.split('\n')
    val ep = expected.split('\n')
    val compare = rs.zip(ep).map {
      case (resultLine, expectedLine) =>
        if (resultLine != expectedLine)
          "DIFF >>>>>>>>>>>\n" +
            s"Result > $resultLine\n" +
            s"Expect < $expectedLine"
        else
          s"Result > $resultLine"
    }.mkString("\n")

    val left = ep.takeRight(ep.length - rs.length).mkString("\n")

    sys.error(
      s"""Swagger.json is off.
         $compare

         >>>>> extra expected lines:
         $left
       """.stripMargin
    )
  }
}

TaskKey[Unit]("unzip1") := {
  val from = new File(s"target/scala-2.12/app_2.12-${version.value}.jar")
  val to = new File("target/jar")
  IO.unzip(from, to)
}

TaskKey[Unit]("unzip2") := {
  val from = new File(s"target/universal/app-${version.value}.zip")
  val to = new File("target/dist")
  IO.unzip(from, to)
}

TaskKey[Unit]("unzip3") := {
  val from = new File(s"target/dist/app-${version.value}/lib/app.app-${version.value}-sans-externalized.jar")
  val to = new File("target/dist/jar")
  IO.unzip(from, to)
}

TaskKey[Unit]("unzip4") := {
  val from = new File(s"target/universal/stage/lib/app.app-${version.value}-sans-externalized.jar")
  val to = new File("target/jar")
  IO.unzip(from, to)
}
