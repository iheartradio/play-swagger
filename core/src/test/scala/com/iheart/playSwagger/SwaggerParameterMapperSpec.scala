package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import com.iheart.playSwagger.Domain.SwaggerParameter
import play.routes.compiler.Parameter

class SwaggerParameterMapperSpec extends Specification {
  "mapParam" >> {
    import SwaggerParameterMapper.mapParam
    implicit val cl = this.getClass.getClassLoader

    "map org.joda.time.DateTime to integer with format epoch" >> {
      mapParam(Parameter("fieldWithDateTime", "org.joda.time.DateTime", None, None)) === SwaggerParameter(
        name = "fieldWithDateTime",
        `type` = Option("integer"),
        format = Option("epoch")
      )
    }

    "override mapping to map DateTime to string with format date-time" >> {
      "single DateTime" >> {
        val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("org.joda.time.DateTime", "string", Some("date-time")))
        val parameter = mapParam(Parameter("fieldWithDateTimeOverRide", "org.joda.time.DateTime", None, None), mappingOverrides = mapOverride)
        parameter.name mustEqual "fieldWithDateTimeOverRide"
        parameter.`type` must beSome("string")
        parameter.format must beSome("date-time")
      }

      "sequence of DateTimes" >> {
        val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("org.joda.time.DateTime", "string", Some("date-time")))
        val parameter = mapParam(Parameter("seqWithDateTimeOverRide", "Option[Seq[org.joda.time.DateTime]]", None, None), mappingOverrides = mapOverride)

        parameter.name mustEqual "seqWithDateTimeOverRide"
        parameter.required must beFalse
        parameter.`type` must beSome("array")
        parameter.items.isDefined must beTrue
        parameter.items.get.name mustEqual "seqWithDateTimeOverRide"
        parameter.items.get.required must beTrue
        parameter.items.get.`type` must beSome("string")
        parameter.items.get.format must beSome("date-time")
      }
    }

    "map java.util.Date to string with format date-time" >> {
      val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("java.util.Date", "string", Some("date-time")))
      val parameter = mapParam(Parameter("fieldWithDate", "java.util.Date", None, None), mappingOverrides = mapOverride)
      parameter.name mustEqual "fieldWithDate"
      parameter.`type` must beSome("string")
      parameter.format must beSome("date-time")
    }

    "map java.time.LocalDate to string with no format" >> {
      val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("java.time.LocalDate", "string"))
      val parameter = mapParam(Parameter("fieldWithLocalDate", "java.time.LocalDate", None, None), mappingOverrides = mapOverride)
      parameter.name mustEqual "fieldWithLocalDate"
      parameter.`type` must beSome("string")
      parameter.format must beNone
    }

    "map Any to any with example value" >> {
      mapParam(Parameter("fieldWithAny", "Any", None, None)) === SwaggerParameter(
        name = "fieldWithAny",
        `type` = Option("any"),
        example = Option(JsString("any JSON value"))
      )
    }

    //TODO: for sequences, should the nested required be ignored?
    "map Option[Seq[T]] to item type" >> {
      mapParam(Parameter("aField", "Option[Seq[String]]", None, None)) === SwaggerParameter(
        name = "aField",
        required = false,
        `type` = Some("array"),
        items = Some(SwaggerParameter(
          name = "aField",
          required = true,
          `type` = Some("string")
        ))
      )
    }

    "map String to string without override interference" >> {
      val mapOverride: Seq[SwaggerMapping] = Seq(
        SwaggerMapping("java.time.LocalDate", "string", Some("date")),
        SwaggerMapping("java.time.Duration", "integer")
      )
      val parameter = mapParam(Parameter("strField", "String", None, None), mappingOverrides = mapOverride)
      parameter.name mustEqual "strField"
      parameter.`type` must beSome("string")
      parameter.format must beNone
    }
  }
}

