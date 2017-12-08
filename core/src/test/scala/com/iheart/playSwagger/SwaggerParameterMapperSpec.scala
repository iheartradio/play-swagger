package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json.{ Json, JsString }
import com.iheart.playSwagger.Domain._
import play.routes.compiler.Parameter

class SwaggerParameterMapperSpec extends Specification {
  "mapParam" >> {
    import SwaggerParameterMapper.mapParam
    implicit val cl = this.getClass.getClassLoader

    "map org.joda.time.DateTime to integer with format epoch" >> {
      mapParam(Parameter("fieldWithDateTime", "org.joda.time.DateTime", None, None)) === GenSwaggerParameter(
        name = "fieldWithDateTime",
        `type` = Option("integer"),
        format = Option("epoch"))
    }

    "override mapping to map DateTime to string with format date-time" >> {
      "single DateTime" >> {
        val specAsParameter = List(Json.obj("type" → "string", "format" → "date-time"))
        val mappings: CustomMappings = List(CustomTypeMapping(
          "org.joda.time.DateTime",
          specAsParameter = specAsParameter))

        val parameter = mapParam(Parameter("fieldWithDateTimeOverRide", "org.joda.time.DateTime", None, None), customMappings = mappings)
        parameter.name mustEqual "fieldWithDateTimeOverRide"
        parameter must beAnInstanceOf[CustomSwaggerParameter]
        parameter.asInstanceOf[CustomSwaggerParameter].specAsParameter === specAsParameter
      }

      "sequence of DateTimes" >> {
        val specAsProperty = Json.obj("type" → "string", "format" → "date-time")
        val mappings: CustomMappings = List(CustomTypeMapping(
          "org.joda.time.DateTime",
          specAsProperty = Some(specAsProperty)))
        val parameter = mapParam(Parameter("seqWithDateTimeOverRide", "Option[Seq[org.joda.time.DateTime]]", None, None), customMappings = mappings).asInstanceOf[GenSwaggerParameter]

        parameter.name mustEqual "seqWithDateTimeOverRide"
        parameter.required must beFalse
        parameter.`type` must beSome("array")
        parameter.items.isDefined must beTrue
        parameter.items.get must beAnInstanceOf[CustomSwaggerParameter]
        val itemsSpec = parameter.items.get.asInstanceOf[CustomSwaggerParameter]
        itemsSpec.name mustEqual "seqWithDateTimeOverRide"
        itemsSpec.specAsProperty === Some(specAsProperty)
      }
    }

    "add new custom type mapping" >> {
      val specAsParameter = List(Json.obj("type" → "string", "format" → "date-time"))
      val mappings: CustomMappings = List(CustomTypeMapping(
        "java.util.Date",
        specAsParameter = specAsParameter))
      val parameter = mapParam(Parameter("fieldWithDate", "java.util.Date", None, None), customMappings = mappings)
      parameter.name mustEqual "fieldWithDate"
      parameter must beAnInstanceOf[CustomSwaggerParameter]
      parameter.asInstanceOf[CustomSwaggerParameter].specAsParameter === specAsParameter
    }

    "map Any to any with example value" >> {
      mapParam(Parameter("fieldWithAny", "Any", None, None)) === GenSwaggerParameter(
        name = "fieldWithAny",
        `type` = Option("any"),
        example = Option(JsString("any JSON value")))
    }

    "map java enum to enum constants" >> {
      mapParam(Parameter("javaEnum", "com.iheart.playSwagger.SampleJavaEnum", None, None)) === GenSwaggerParameter(
        name = "javaEnum",
        `type` = Option("string"),
        enum = Option(Seq("DISABLED", "ACTIVE")))
    }

    "map scala enum to enum constants" >> {
      mapParam(Parameter("scalaEnum", "com.iheart.playSwagger.SampleScalaEnum.Value", None, None)) === GenSwaggerParameter(
        name = "scalaEnum",
        `type` = Option("string"),
        enum = Option(Seq("One", "Two")))
    }

    //TODO: for sequences, should the nested required be ignored?
    "map Option[Seq[T]] to item type" >> {
      mapParam(Parameter("aField", "Option[Seq[String]]", None, None)) === GenSwaggerParameter(
        name = "aField",
        required = false,
        `type` = Some("array"),
        items = Some(GenSwaggerParameter(
          name = "aField",
          required = true,
          `type` = Some("string"))))
    }

    "map String to string without override interference" >> {

      val specAsParameter = List(Json.obj("type" → "string", "format" → "date-time"))
      val mappings: CustomMappings = List(CustomTypeMapping(
        "java.time.LocalDate",
        specAsParameter = specAsParameter), CustomTypeMapping(
        "java.time.Duration",
        specAsParameter = specAsParameter))
      val parameter = mapParam(Parameter("strField", "String", None, None), customMappings = mappings)
      parameter.name mustEqual "strField"
      parameter must beAnInstanceOf[GenSwaggerParameter]
      parameter.asInstanceOf[GenSwaggerParameter].`type` must beSome("string")
      parameter.asInstanceOf[GenSwaggerParameter].format must beNone
    }

    "map default value to content without quotes when provided with string without quotes" >> {
      mapParam(Parameter("strField", "String", None, Some("defaultValue"))) === GenSwaggerParameter(
        name = "strField",
        `type` = Option("string"),
        required = false,
        default = Option(JsString("defaultValue")))
    }
    "map default value to content without quotes when provided with string with simple quotes" >> {
      mapParam(Parameter("strField", "String", None, Some("\"defaultValue\""))) === GenSwaggerParameter(
        name = "strField",
        `type` = Option("string"),
        required = false,
        default = Option(JsString("defaultValue")))
    }
    "map default value to content without quotes when provided with string with triple quotes" >> {
      mapParam(Parameter("strField", "String", None, Some("\"\"\"defaultValue\"\"\""))) === GenSwaggerParameter(
        name = "strField",
        `type` = Option("string"),
        required = false,
        default = Option(JsString("defaultValue")))
    }
    "map definition name to as it is" >> {
      mapParam(Parameter("field_WithAny", "Any", None, None), new NoTransformer) === GenSwaggerParameter(
        name = "field_WithAny",
        `type` = Option("any"),
        example = Option(JsString("any JSON value")))
    }
  }
}

