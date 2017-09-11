import org.json4s._

import org.json4s.native.JsonMethods._
import org.json4s.native.JsonMethods
logLevel in update := sbt.Level.Warn

enablePlugins(PlayScala, SwaggerPlugin)

name := "app"

scalaVersion := "2.11.11"

swaggerDomainNameSpaces := Seq("namespace1", "namespace2")

swaggerRoutesFile := "my-routes"

swaggerOutputTransformers := Seq(envOutputTransformer)

val pathVal = System.getenv("PATH")

TaskKey[Unit]("check") := {

  def uniform(jsString: String): String = pretty(render(parse(jsString)))
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
      |               "type":"string"
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
      |      "title":"Poweramp API",
      |      "description":"My API is the best"
      |   },
      |   "tags":[
      |      {
      |         "name":"${swaggerRoutesFile.value}"
      |      }
      |   ]
      |}
    """.stripMargin)

  val result = uniform(IO.read(swaggerTarget.value / swaggerFileName.value))


  if (result != expected) {
    val rs = result.split('\n')
    val ep = expected.split('\n')
    val compare = rs.zip(ep).map {
      case (resultLine, expectedLine) =>
        if(resultLine != expectedLine)
          "DIFF >>>>>>>>>>>\n" +
          s"Result > $resultLine\n" +
          s"Expect < $expectedLine"
        else
          s"Result > $resultLine"
    }.mkString("\n")

    val left = ep.takeRight(ep.size - rs.size).mkString("\n")

    sys.error(
      s"""Swagger.json is off.
         $compare

         >>>>> extra expected lines:
         $left
       """.stripMargin)
  }
}

TaskKey[Unit]("unzip1") := {
  val from = new File("target/scala-2.11/app_2.11-0.1-SNAPSHOT.jar")
  val to = new File("target/jar")
  IO.unzip(from, to)
}

TaskKey[Unit]("unzip2") := {
  val from = new File("target/universal/app-0.1-SNAPSHOT.zip")
  val to = new File("target/dist")
  IO.unzip(from, to)
}

TaskKey[Unit]("unzip3") := {
  val from = new File("target/dist/app-0.1-SNAPSHOT/lib/app.app-0.1-SNAPSHOT-sans-externalized.jar")
  val to = new File("target/dist/jar")
  IO.unzip(from, to)
}

TaskKey[Unit]("unzip4") := {
  val from = new File("target/universal/stage/lib/app.app-0.1-SNAPSHOT-sans-externalized.jar")
  val to = new File("target/jar")
  IO.unzip(from, to)
}