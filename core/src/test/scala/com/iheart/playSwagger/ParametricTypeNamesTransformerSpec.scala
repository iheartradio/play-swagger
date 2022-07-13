package com.iheart.playSwagger

import scala.util.Success

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ParametricTypeNamesTransformerSpec extends Specification {
  private val transformer = new ParametricTypeNamesTransformer

  transformer.getClass.getSimpleName >> {
    "transform parametrized declaration into RFC-3986 compliant URI identifiers" >> {
      transformer {
        Json.obj(
          "$ref" -> "#/components/schemas/some.class.Name[SomeType, another.ClassName]",
          "types" -> Json.obj(
            "another.class.Name[String, Seq[Option[Int]]]" -> 3,
            "arrField" -> Json.arr(
              "1" -> "should.not.be.replaced.ClassName[String]",
              "2" -> "should.not.be.replaced.ClassName[Int]"
            )
          )
        )
      } === Success(
        Json.obj(
          "$ref" -> "#/components/schemas/some.class.Name-SomeType_another.ClassName",
          "types" -> Json.obj(
            "another.class.Name-String_Seq-Option-Int" -> 3,
            "arrField" -> Json.arr(
              "1" -> "should.not.be.replaced.ClassName[String]",
              "2" -> "should.not.be.replaced.ClassName[Int]"
            )
          )
        )
      )
    }
  }
}
