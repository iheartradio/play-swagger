package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import com.iheart.playSwagger.Domain.SwaggerParameter
import play.routes.compiler.Parameter

class SwaggerParameterMapperSpec extends Specification {
  "mapParam" >> {
    import SwaggerParameterMapper.mapParam

    "map org.joda.time.DateTime to integer with format epoch" >> {
      mapParam(Parameter("fieldWithDateTime", "org.joda.time.DateTime", None, None)) === SwaggerParameter(
        name = "fieldWithDateTime",
        `type` = Option("integer"),
        format = Option("epoch")
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
  }
}

