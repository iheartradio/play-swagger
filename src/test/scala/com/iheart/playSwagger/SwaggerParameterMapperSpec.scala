package com.iheart.playSwagger

import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import com.iheart.playSwagger.Domain.SwaggerParameter

class SwaggerParameterMapperSpec extends Specification {
  "mapParam" >> {
    import SwaggerParameterMapper.mapParam

    "map org.jodaTime.DateTime to integer with format epoch" >> {
      mapParam("fieldWithDateTime", "org.jodaTime.DateTime") === SwaggerParameter(
        name = "fieldWithDateTime",
        `type` = Option("integer"),
        format = Option("epoch")
      )
    }

    "map Any to any with example value" >> {
      mapParam("fieldWithAny", "Any") === SwaggerParameter(
        name = "fieldWithAny",
        `type` = Option("any"),
        example = Option(JsString("any JSON value"))
      )
    }

    "map Option[Seq[T]] to item type" >> {
      mapParam("aField", "Option[Seq[String]]") === SwaggerParameter(
        name = "aField",
        required = false,
        items = Some("String")
      )
    }
  }
}

