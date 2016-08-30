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
        implicit val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("org.joda.time.DateTime", "string", Some("date-time")))
        mapParam(Parameter("fieldWithDateTimeOverRide", "org.joda.time.DateTime", None, None)) == SwaggerParameter(
          name = "fieldWithDateTimeOverRide",
          `type` = Option("string"),
          format = Option("date-time")
        )
      }

      "sequence of DateTimes" >> {
        implicit val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("org.joda.time.DateTime", "string", Some("date-time")))
        mapParam(Parameter("seqWithDateTimeOverRide", "Option[Seq[org.joda.time.DateTime]]", None, None)) == SwaggerParameter(
          name = "seqWithDateTimeOverRide",
          required = false,
          `type` = Some("array"),
          items = Some(SwaggerParameter(
            name = "seqWithDateTimeOverRide",
            required = true,
            `type` = Option("string"),
            format = Option("date-time")
          ))
        )
      }
    }

    "map java.util.Date to string with format date-time" >> {
      implicit val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("java.util.Date", "string", Some("date-time")))
      mapParam(Parameter("fieldWithDate", "java.util.Date", None, None)) == SwaggerParameter(
        name = "fieldWithDate",
        `type` = Option("string"),
        format = Option("date-time")
      )
    }

    "map java.time.LocalDate to string with no format" >> {
      implicit val mapOverride: Seq[SwaggerMapping] = Seq(SwaggerMapping("java.time.LocalDate", "string"))
      mapParam(Parameter("fieldWithLocalDate", "java.time.LocalDate", None, None)) == SwaggerParameter(
        name = "fieldWithLocalDate",
        `type` = Option("string"),
        format = None
      )
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

    "map String to string" >> {
      // Ensure the overrides don't mess anything up
      implicit val mapOverride: Seq[SwaggerMapping] = Seq(
        SwaggerMapping("java.time.LocalDate", "string", Some("date")),
        SwaggerMapping("java.time.Duration", "integer")
      )
      mapParam(Parameter("strField", "String", None, None)) === SwaggerParameter(
        name = "strField",
        `type` = Some("string"),
        format = None
      )
    }
  }
}

