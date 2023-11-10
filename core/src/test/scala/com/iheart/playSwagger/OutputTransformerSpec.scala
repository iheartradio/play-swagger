package com.iheart.playSwagger

import scala.util.{Failure, Success}

import com.iheart.playSwagger.OutputTransformer.SimpleOutputTransformer
import com.iheart.playSwagger.generator.{NamingConvention, PrefixDomainModelQualifier, SwaggerSpecGenerator}
import org.specs2.mutable.Specification
import play.api.libs.json._

class OutputTransformerSpec extends Specification {
  "OutputTransformer.traverseTransformer" >> {

    "traverse and transform object and update simple paths" >> {
      val result = OutputTransformer.traverseTransformer(Json.obj(
        "a" -> 1,
        "b" -> "c"
      )) { _ => Success(JsNumber(10)) }
      result === Success(Json.obj("a" -> 10, "b" -> 10))
    }

    "traverse and transform object and update nested paths" >> {
      val result = OutputTransformer.traverseTransformer(Json.obj(
        "a" -> 1,
        "b" -> Json.obj(
          "c" -> 1
        )
      )) { _ => Success(JsNumber(10)) }
      result === Success(Json.obj("a" -> 10, "b" -> Json.obj("c" -> 10)))
    }

    "traverse and transform object and update array paths" >> {
      val result = OutputTransformer.traverseTransformer(Json.obj(
        "a" -> 1,
        "b" -> Json.arr(
          Json.obj("c" -> 1),
          Json.obj("d" -> 1),
          Json.obj("e" -> 1)
        )
      )) { _ => Success(JsNumber(10)) }
      result === Success(Json.obj(
        "a" -> 10,
        "b" -> Json.arr(
          Json.obj("c" -> 10),
          Json.obj("d" -> 10),
          Json.obj("e" -> 10)
        )
      ))
    }

    "return a failure when there's a problem transforming data" >> {
      val err: IllegalArgumentException = new scala.IllegalArgumentException("failed")
      val result = OutputTransformer.traverseTransformer(Json.obj(
        "a" -> 1,
        "b" -> Json.obj(
          "c" -> 1
        )
      )) { _ => Failure(err) }
      result === Failure(err)
    }
  }
  "OutputTransformer.>=>" >> {
    "return composed function" >> {
      val a = SimpleOutputTransformer(OutputTransformer.traverseTransformer(_) {
        case JsString(content) => Success(JsString(content + "a"))
        case _ => Failure(new IllegalStateException())
      })
      val b = SimpleOutputTransformer(OutputTransformer.traverseTransformer(_) {
        case JsString(content) => Success(JsString(content + "b"))
        case _ => Failure(new IllegalStateException())
      })

      val g = a >=> b
      g(Json.obj(
        "A" -> "Z",
        "B" -> "Y"
      )) must beSuccessfulTry.withValue(Json.obj(
        "A" -> "Zab",
        "B" -> "Yab"
      ))
    }

    "fail if one composed function fails" >> {
      val a = SimpleOutputTransformer(OutputTransformer.traverseTransformer(_) {
        case JsString(content) => Success(JsString("a" + content))
        case _ => Failure(new IllegalStateException())
      })
      val b = SimpleOutputTransformer(OutputTransformer.traverseTransformer(_) {
        case JsString(_) => Failure(new IllegalStateException("not strings"))
        case _ => Failure(new IllegalStateException())
      })

      val g = a >=> b
      g(Json.obj(
        "A" -> "Z",
        "B" -> "Y"
      )) must beFailedTry[JsObject].withThrowable[IllegalStateException]("not strings")
    }
  }
}

class EnvironmentVariablesSpec extends Specification {
  "EnvironmentVariables" >> {
    "transform json with markup values" >> {
      val envs = Map("A" -> "B", "C" -> "D")
      val instance = MapVariablesTransformer(envs)
      instance(Json.obj(
        "a" -> "${A}",
        "b" -> Json.obj(
          "c" -> "${C}"
        )
      )) === Success(Json.obj("a" -> "B", "b" -> Json.obj("c" -> "D")))
    }

    "return failure when using non present environment variables" >> {
      val envs = Map("A" -> "B", "C" -> "D")
      val instance = MapVariablesTransformer(envs)
      instance(Json.obj(
        "a" -> "${A}",
        "b" -> Json.obj(
          "c" -> "${NON_EXISTING}"
        )
      )) must beFailedTry[JsObject].withThrowable[IllegalStateException]("Unable to find variable NON_EXISTING")
    }
  }
}

class EnvironmentVariablesIntegrationSpec extends Specification {
  implicit val cl: ClassLoader = getClass.getClassLoader

  "integration" >> {
    "generate api with placeholders in place" >> {
      val envs = Map("LAST_TRACK_DESCRIPTION" -> "Last track", "PLAYED_TRACKS_DESCRIPTION" -> "Add tracks")
      val json = generator.SwaggerSpecGenerator(
        NamingConvention.None,
        PrefixDomainModelQualifier("com.iheart"),
        outputTransformers = MapVariablesTransformer(envs) :: Nil
      ).generate("env.routes").get
      val pathJson = json \ "paths"
      val stationJson = (pathJson \ "/api/station/{sid}/playedTracks/last" \ "get").as[JsObject]
      val addTrackJson = (pathJson \ "/api/station/playedTracks" \ "post").as[JsObject]

      ((addTrackJson \ "parameters").as[JsArray].value.head \ "description").as[String] === "Add tracks"
      (stationJson \ "responses" \ "200" \ "description").as[String] === "Last track"
    }
  }

  "fail to generate API if environment variable is not found" >> {
    val envs = Map("LAST_TRACK_DESCRIPTION" -> "Last track")
    val json = SwaggerSpecGenerator(
      namingConvention = NamingConvention.None,
      modelQualifier = PrefixDomainModelQualifier("com.iheart"),
      outputTransformers = MapVariablesTransformer(envs) :: Nil
    ).generate("env.routes")
    json must beFailedTry[JsObject].withThrowable[IllegalStateException](
      "Unable to find variable PLAYED_TRACKS_DESCRIPTION"
    )
  }
}
