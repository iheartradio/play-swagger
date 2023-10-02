package com.iheart.playSwagger
import com.iheart.playSwagger.domain.CustomTypeMapping
import com.iheart.playSwagger.domain.parameter.{CustomSwaggerParameter, GenSwaggerParameter}
import com.iheart.playSwagger.generator.{PrefixDomainModelQualifier, SwaggerParameterMapper}
import org.specs2.mutable.Specification
import play.api.libs.json.{JsString, Json}
import play.routes.compiler.Parameter

class SwaggerParameterMapperSpec extends Specification {
  "mapParam" >> {
    val generalMapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier())
    implicit val cl: ClassLoader = this.getClass.getClassLoader

    "map org.joda.time.DateTime to integer with format epoch" >> {
      generalMapper.mapParam(
        Parameter("fieldWithDateTime", "org.joda.time.DateTime", None, None),
        None
      ) === GenSwaggerParameter(
        name = "fieldWithDateTime",
        required = true,
        `type` = Option("integer"),
        format = Option("epoch")
      )
    }

    "override mapping to map DateTime to string with format date-time" >> {
      "single DateTime" >> {
        val specAsParameter = List(Json.obj("type" -> "string", "format" -> "date-time"))
        val mappings: Seq[CustomTypeMapping] = List(CustomTypeMapping(
          "org.joda.time.DateTime",
          specAsParameter = specAsParameter
        ))

        val mapper = new SwaggerParameterMapper(mappings, PrefixDomainModelQualifier())

        val parameter = mapper.mapParam(
          Parameter("fieldWithDateTimeOverRide", "org.joda.time.DateTime", None, None),
          None
        )
        parameter.name mustEqual "fieldWithDateTimeOverRide"
        parameter must beAnInstanceOf[CustomSwaggerParameter]
        parameter.asInstanceOf[CustomSwaggerParameter].specAsParameter === specAsParameter
      }

      "sequence of DateTimes" >> {
        val specAsProperty = Json.obj("type" -> "string", "format" -> "date-time")
        val mappings: Seq[CustomTypeMapping] = List(CustomTypeMapping(
          "org.joda.time.DateTime",
          specAsProperty = Some(specAsProperty)
        ))

        val mapper = new SwaggerParameterMapper(mappings, PrefixDomainModelQualifier())

        val parameter = mapper.mapParam(
          Parameter("seqWithDateTimeOverRide", "Option[Seq[org.joda.time.DateTime]]", None, None),
          None
        ).asInstanceOf[GenSwaggerParameter]

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
      val specAsParameter = List(Json.obj("type" -> "string", "format" -> "date-time"))
      val mappings: Seq[CustomTypeMapping] = List(CustomTypeMapping(
        "java.util.Date",
        specAsParameter = specAsParameter
      ))

      val mapper = new SwaggerParameterMapper(mappings, PrefixDomainModelQualifier())

      val parameter = mapper.mapParam(Parameter("fieldWithDate", "java.util.Date", None, None), None)
      parameter.name mustEqual "fieldWithDate"
      parameter must beAnInstanceOf[CustomSwaggerParameter]
      parameter.asInstanceOf[CustomSwaggerParameter].specAsParameter === specAsParameter
    }

    "map Any to any with example value" >> {
      generalMapper.mapParam(Parameter("fieldWithAny", "Any", None, None), None) === GenSwaggerParameter(
        name = "fieldWithAny",
        required = true,
        `type` = Option("any"),
        example = Option(JsString("any JSON value"))
      )
    }

    "map java enum to enum constants" >> {
      generalMapper.mapParam(
        Parameter("javaEnum", "com.iheart.playSwagger.SampleJavaEnum", None, None),
        None
      ) === GenSwaggerParameter(
        name = "javaEnum",
        required = true,
        `type` = Option("string"),
        enum = Option(Seq("DISABLED", "ACTIVE"))
      )
    }

    "map scala enum to enum constants" >> {
      generalMapper.mapParam(
        Parameter("scalaEnum", "com.iheart.playSwagger.SampleScalaEnum.Value", None, None),
        None
      ) === GenSwaggerParameter(
        name = "scalaEnum",
        required = true,
        `type` = Option("string"),
        enum = Option(Seq("One", "Two"))
      )
    }

    // TODO: for sequences, should the nested required be ignored?
    "map Option[Seq[T]] to item type" >> {
      generalMapper.mapParam(Parameter("aField", "Option[Seq[String]]", None, None), None) === GenSwaggerParameter(
        name = "aField",
        required = false,
        nullable = Some(true),
        `type` = Some("array"),
        items = Some(GenSwaggerParameter(
          name = "aField",
          required = true,
          nullable = None,
          `type` = Some("string")
        ))
      )
    }

    "map scala.collection.immutable.Seq[T] to item type" >> {
      generalMapper.mapParam(
        Parameter("aField", "scala.collection.immutable.Seq[String]", None, None),
        None
      ) === GenSwaggerParameter(
        name = "aField",
        required = true,
        nullable = None,
        `type` = Some("array"),
        items = Some(GenSwaggerParameter(
          name = "aField",
          required = true,
          nullable = None,
          `type` = Some("string")
        ))
      )
    }

    "map String to string without override interference" >> {

      val specAsParameter = List(Json.obj("type" -> "string", "format" -> "date-time"))
      val mappings: Seq[CustomTypeMapping] = List(
        CustomTypeMapping(
          "java.time.LocalDate",
          specAsParameter = specAsParameter
        ),
        CustomTypeMapping(
          "java.time.Duration",
          specAsParameter = specAsParameter
        )
      )

      val mapper = new SwaggerParameterMapper(mappings, PrefixDomainModelQualifier())

      val parameter = mapper.mapParam(Parameter("strField", "String", None, None), None)
      parameter.name mustEqual "strField"
      parameter must beAnInstanceOf[GenSwaggerParameter]
      parameter.asInstanceOf[GenSwaggerParameter].`type` must beSome("string")
      parameter.asInstanceOf[GenSwaggerParameter].format must beNone
    }

    "map default value to content without quotes when provided with string without quotes" >> {
      generalMapper.mapParam(Parameter("strField", "String", None, Some("defaultValue")), None) === GenSwaggerParameter(
        name = "strField",
        `type` = Option("string"),
        required = false,
        default = Option(JsString("defaultValue"))
      )
    }
    "map default value to content without quotes when provided with string with simple quotes" >> {
      generalMapper.mapParam(
        Parameter("strField", "String", None, Some("\"defaultValue\"")),
        None
      ) === GenSwaggerParameter(
        name = "strField",
        `type` = Option("string"),
        required = false,
        default = Option(JsString("defaultValue"))
      )
    }
    "map default value to content without quotes when provided with string with triple quotes" >> {
      generalMapper.mapParam(
        Parameter("strField", "String", None, Some("\"\"\"defaultValue\"\"\"")),
        None
      ) === GenSwaggerParameter(
        name = "strField",
        `type` = Option("string"),
        required = false,
        default = Option(JsString("defaultValue"))
      )
    }
  }
}
