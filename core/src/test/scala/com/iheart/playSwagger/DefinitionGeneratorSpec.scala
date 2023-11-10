package com.iheart.playSwagger
import com.iheart.playSwagger.domain.parameter.{CustomSwaggerParameter, GenSwaggerParameter}
import com.iheart.playSwagger.domain.{CustomTypeMapping, Definition}
import com.iheart.playSwagger.generator.{DefinitionGenerator, DomainModelQualifier, NamingConvention, PrefixDomainModelQualifier, SwaggerParameterMapper}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

case class DictType(key: String, value: String)
case class Foo(
    barStr: String,
    barInt: Int,
    barLong: Option[Long],
    reffedFoo: ReffedFoo,
    seqReffedFoo: Seq[ReffedFoo],
    optionSeqReffedFoo: Option[Seq[ReffedFoo]],
    dictType: DictType
)
case class ReffedFoo(name: String, refrefFoo: RefReffedFoo)
case class RefReffedFoo(bar: String)

case class FooWithSeq(seq: Seq[SeqItem])

case class SeqItem(bar: String)

case class FooWithWrappedStringProperties(
    required: WrappedString,
    optional: Option[WrappedString],
    seq: Seq[WrappedString]
)
case class WrappedString(value: String)

case class FooWithSeq2(abc1: Seq[Bar.Bar], abc2: Seq[Seq[Bar.Foo]])

object Bar {
  type Bar = String
  type Foo = Int
}

case class WithListOfPrimitive(seq: Seq[Int])

case class FooWithOption(op: Option[OptionItem])
case class OptionItem(bar: String)

case class WithDate(someDate: org.joda.time.DateTime)
case class WithOptionalDate(someDate: Option[org.joda.time.DateTime])

object MyObject {
  type MyId = Int
  case class MyInnerClass(bar: String, id: MyId)
}

object ExcludingDomainQualifier extends DomainModelQualifier {
  val parent: PrefixDomainModelQualifier = PrefixDomainModelQualifier("com.iheart.playSwagger")
  val exclusions: Seq[String] = Seq("com.iheart.playSwagger.DictType")
  override def isModel(className: String): Boolean = parent.isModel(className) && !(exclusions contains className)
}

class DefinitionGeneratorSpec extends Specification {
  implicit val cl: ClassLoader = getClass.getClassLoader
  val generalMapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier())

  "definition" >> {

    "generate name correctly" >> {
      DefinitionGenerator(generalMapper).definition[Foo].name === "com.iheart.playSwagger.Foo"
    }

    "generate from string classname " >> {
      DefinitionGenerator(generalMapper).definition("com.iheart.playSwagger.Foo").name === "com.iheart.playSwagger.Foo"
    }

    "generate properties" >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart.playSwagger"))
      val result = DefinitionGenerator(
        mapper,
        NamingConvention.None,
        embedScaladoc = false
      ).definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === GenSwaggerParameter(name = "barStr", required = true, `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === GenSwaggerParameter(
          name = "barInt",
          required = true,
          `type` = Some("integer"),
          format = Some("int32")
        )
      }

      "with correct optional long property" >> {
        result(2) === GenSwaggerParameter(
          name = "barLong",
          `type` = Some("integer"),
          format = Some("int64"),
          required = false,
          nullable = Some(true)
        )
      }

      "with reference type" >> {
        result(3) === GenSwaggerParameter(
          name = "reffedFoo",
          required = true,
          referenceType = Some("com.iheart.playSwagger.ReffedFoo")
        )
      }

      "with sequence of reference type" >> {
        val itemsParam =
          GenSwaggerParameter(
            name = "seqReffedFoo",
            required = true,
            referenceType = Some("com.iheart.playSwagger.ReffedFoo")
          )
        result(4) === GenSwaggerParameter(
          name = "seqReffedFoo",
          required = true,
          `type` = Some("array"),
          items = Some(itemsParam)
        )
      }

      "with optional sequence of reference type" >> {
        val itemsParam =
          GenSwaggerParameter(
            name = "optionSeqReffedFoo",
            required = true,
            referenceType = Some("com.iheart.playSwagger.ReffedFoo")
          )
        result(5) === GenSwaggerParameter(
          name = "optionSeqReffedFoo",
          `type` = Some("array"),
          items = Some(itemsParam),
          required = false,
          nullable = Some(true)
        )
      }

    }

    "generate properties using snake case naming strategy" >> {

      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart.playSwagger"))
      val result =
        DefinitionGenerator(mapper, NamingConvention.SnakeCase, embedScaladoc = false).definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === GenSwaggerParameter(name = "bar_str", required = true, `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === GenSwaggerParameter(
          name = "bar_int",
          required = true,
          `type` = Some("integer"),
          format = Some("int32")
        )
      }

      "with correct optional long property" >> {
        result(2) === GenSwaggerParameter(
          name = "bar_long",
          `type` = Some("integer"),
          format = Some("int64"),
          required = false,
          nullable = Some(true)
        )
      }

      "with reference type" >> {
        result(3) === GenSwaggerParameter(
          name = "reffed_foo",
          required = true,
          referenceType = Some("com.iheart.playSwagger.ReffedFoo")
        )
      }

      "with sequence of reference type" >> {
        val itemsParam =
          GenSwaggerParameter(
            name = "seq_reffed_foo",
            required = true,
            referenceType = Some("com.iheart.playSwagger.ReffedFoo")
          )
        result(4) === GenSwaggerParameter(
          name = "seq_reffed_foo",
          required = true,
          `type` = Some("array"),
          items = Some(itemsParam)
        )
      }

      "with optional sequence of reference type" >> {
        val itemsParam =
          GenSwaggerParameter(
            name = "option_seq_reffed_foo",
            required = true,
            referenceType = Some("com.iheart.playSwagger.ReffedFoo")
          )
        result(5) === GenSwaggerParameter(
          name = "option_seq_reffed_foo",
          `type` = Some("array"),
          items = Some(itemsParam),
          required = false,
          nullable = Some(true)
        )
      }

    }

    "generate properties using kebab case naming strategy" >> {

      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart.playSwagger"))
      val result =
        DefinitionGenerator(mapper, NamingConvention.KebabCase, embedScaladoc = false).definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === GenSwaggerParameter(name = "bar-str", required = true, `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === GenSwaggerParameter(
          name = "bar-int",
          required = true,
          `type` = Some("integer"),
          format = Some("int32")
        )
      }

      "with correct optional long property" >> {
        result(2) === GenSwaggerParameter(
          name = "bar-long",
          `type` = Some("integer"),
          format = Some("int64"),
          required = false,
          nullable = Some(true)
        )
      }

      "with reference type" >> {
        result(3) === GenSwaggerParameter(
          name = "reffed-foo",
          required = true,
          referenceType = Some("com.iheart.playSwagger.ReffedFoo")
        )
      }

      "with sequence of reference type" >> {
        val itemsParam =
          GenSwaggerParameter(
            name = "seq-reffed-foo",
            required = true,
            referenceType = Some("com.iheart.playSwagger.ReffedFoo")
          )
        result(4) === GenSwaggerParameter(
          name = "seq-reffed-foo",
          required = true,
          `type` = Some("array"),
          items = Some(itemsParam)
        )
      }

      "with optional sequence of reference type" >> {
        val itemsParam =
          GenSwaggerParameter(
            name = "option-seq-reffed-foo",
            required = true,
            referenceType = Some("com.iheart.playSwagger.ReffedFoo")
          )
        result(5) === GenSwaggerParameter(
          name = "option-seq-reffed-foo",
          `type` = Some("array"),
          items = Some(itemsParam),
          required = false,
          nullable = Some(true)
        )
      }

    }

    "read class in Object" >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart"))
      val result = DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
        "com.iheart.playSwagger.MyObject.MyInnerClass"
      )
      result.properties.head.name === "bar"
    }

    "read alias type in Object" >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart"))
      val result = DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
        "com.iheart.playSwagger.MyObject.MyInnerClass"
      )

      val last = result.properties.last.asInstanceOf[GenSwaggerParameter]
      last.name === "id"
      last.`type` === Some("integer")
      last.referenceType === None
    }

    "read sequence items" >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart"))
      val result =
        DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
          "com.iheart.playSwagger.FooWithSeq"
        )
      result.properties.head.asInstanceOf[GenSwaggerParameter].items.get.asInstanceOf[
        GenSwaggerParameter
      ].referenceType === Some("com.iheart.playSwagger.SeqItem")
    }

    "read primitive sequence items" >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart"))
      val result = DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
        "com.iheart.playSwagger.WithListOfPrimitive"
      )
      result.properties.head.asInstanceOf[GenSwaggerParameter].items.get.asInstanceOf[
        GenSwaggerParameter
      ].`type` === Some("integer")

    }

    "read Optional items " >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart"))
      val result =
        DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
          "com.iheart.playSwagger.FooWithOption"
        )
      result.properties.head.asInstanceOf[GenSwaggerParameter].referenceType must beSome(
        "com.iheart.playSwagger.OptionItem"
      )
    }

    "with dates" >> {
      "no override" >> {
        val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart"))
        val result =
          DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
            "com.iheart.playSwagger.WithDate"
          )
        val prop = result.properties.head.asInstanceOf[GenSwaggerParameter]
        prop.`type` must beSome("integer")
        prop.format must beSome("epoch")

      }
      "with override" >> {
        val customJson = List(Json.obj("type" -> "string", "format" -> "date-time"))
        val mappings = List(
          CustomTypeMapping(
            `type` = "org.joda.time.DateTime",
            specAsParameter = customJson
          )
        )
        val mapper = new SwaggerParameterMapper(mappings, PrefixDomainModelQualifier("com.iheart"))
        val result =
          DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
            "com.iheart.playSwagger.WithDate"
          )
        val prop = result.properties.head.asInstanceOf[CustomSwaggerParameter]
        prop.specAsParameter === customJson
      }
    }
    "with optional date" >> {
      "with override" >> {
        val customJson = List(Json.obj("type" -> "string", "format" -> "date-time"))
        val mappings = List(
          CustomTypeMapping(
            `type` = "org.joda.time.DateTime",
            specAsParameter = customJson
          )
        )
        val mapper = new SwaggerParameterMapper(mappings, PrefixDomainModelQualifier("com.iheart"))
        val result = DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false).definition(
          "com.iheart.playSwagger.WithOptionalDate"
        )
        val prop = result.properties.head.asInstanceOf[CustomSwaggerParameter]
        prop.specAsParameter === customJson
        prop.nullable === Some(true)
      }
    }

    "with property overrides" >> {
      val customJson = List(Json.obj("type" -> "string"))
      val customMapping = CustomTypeMapping(
        `type` = "com.iheart.playSwagger.WrappedString",
        specAsParameter = customJson
      )
      val mapper = new SwaggerParameterMapper(List(customMapping), PrefixDomainModelQualifier("com.iheart"))
      val generator = DefinitionGenerator(mapper, NamingConvention.None, embedScaladoc = false)
      val definition = generator.definition[FooWithWrappedStringProperties]

      "support simple property types" >> {
        val requiredParam = definition.properties.find(_.name == "required").get
        requiredParam must beAnInstanceOf[CustomSwaggerParameter]
        val parameter = requiredParam.asInstanceOf[CustomSwaggerParameter]
        parameter.required must beTrue
        parameter.specAsParameter mustEqual customJson
      }

      "support optional property types" >> {
        val optionalParam = definition.properties.find(_.name == "optional").get
        optionalParam must beAnInstanceOf[CustomSwaggerParameter]
        val parameter = optionalParam.asInstanceOf[CustomSwaggerParameter]
        parameter.required must beFalse
        parameter.specAsParameter mustEqual customJson
      }

      "support element overrides in seq" >> {
        val seqParam = definition.properties.find(_.name == "seq").get
        seqParam must beAnInstanceOf[GenSwaggerParameter]
        seqParam.asInstanceOf[GenSwaggerParameter].items must beSome(beAnInstanceOf[CustomSwaggerParameter])
        val items = seqParam.asInstanceOf[GenSwaggerParameter].items.get.asInstanceOf[CustomSwaggerParameter]
        items.specAsParameter mustEqual customJson
      }
    }
  }

  "allDefinitions" >> {
    val mapper = new SwaggerParameterMapper(Nil, ExcludingDomainQualifier)
    val allDefs = DefinitionGenerator(mapper).allDefinitions(List("com.iheart.playSwagger.Foo"))
    allDefs.length === 3
    allDefs.find(_.name == "com.iheart.playSwagger.ReffedFoo") must beSome[Definition]
    allDefs.find(_.name == "com.iheart.playSwagger.RefReffedFoo") must beSome[Definition]
    allDefs.find(_.name == "com.iheart.playSwagger.Foo") must beSome[Definition]
  }

  "java class definition" >> {
    "generate name correctly" >> {
      DefinitionGenerator(generalMapper).definition[Person].name === "com.iheart.playSwagger.Person"
    }

    "generate from string classname " >> {
      DefinitionGenerator(generalMapper).definition(
        "com.iheart.playSwagger.Person"
      ).name === "com.iheart.playSwagger.Person"
    }

    "generate properties" >> {
      val mapper = new SwaggerParameterMapper(Nil, PrefixDomainModelQualifier("com.iheart.playSwagger"))
      val result = DefinitionGenerator(
        mapper,
        swaggerPlayJava = true,
        NamingConvention.None
      ).definition[Person].properties
      result.length === 16

      "with correct long property" >> {
        result.filter(r => r.name == "id").seq.head === GenSwaggerParameter(
          name = "id",
          `type` = Some("integer"),
          format = Some("int64"),
          required = false,
          nullable = Some(true)
        )
        result.filter(r => r.name == "aLong").seq.head === GenSwaggerParameter(
          name = "aLong",
          `type` = Some("integer"),
          format = Some("int64"),
          required = false,
          nullable = Some(true)
        )
      }
      "with correct int32 property" >> {
        result.filter(r => r.name == "integer").seq.head === GenSwaggerParameter(
          name = "integer",
          `type` = Some("integer"),
          format = Some("int32"),
          required = false,
          nullable = Some(true)
        )
        result.filter(r => r.name == "anInt").seq.head === GenSwaggerParameter(
          name = "anInt",
          `type` = Some("integer"),
          format = Some("int32"),
          required = false,
          nullable = Some(true)
        )
      }

      "with correct double property" >> {
        result.filter(r => r.name == "aDouble").seq.head === GenSwaggerParameter(
          name = "aDouble",
          `type` = Some("number"),
          format = Some("double"),
          required = false,
          nullable = Some(true)
        )
        result.filter(r => r.name == "double").seq.head === GenSwaggerParameter(
          name = "double",
          `type` = Some("number"),
          format = Some("double"),
          required = false,
          nullable = Some(true)
        )
      }

      "with correct float property" >> {
        result.filter(r => r.name == "float").seq.head === GenSwaggerParameter(
          name = "float",
          `type` = Some("number"),
          format = Some("float"),
          required = false,
          nullable = Some(true)
        )
        result.filter(r => r.name == "aFloat").seq.head === GenSwaggerParameter(
          name = "aFloat",
          `type` = Some("number"),
          format = Some("float"),
          required = false,
          nullable = Some(true)
        )
      }

      "with correct java time property" >> {
        result.filter(r => r.name == "instant").seq.head === GenSwaggerParameter(
          name = "instant",
          `type` = Some("string"),
          format = Some("date-time"),
          required = false,
          nullable = Some(true)
        )
        result.filter(r => r.name == "dayOfBirth").seq.head === GenSwaggerParameter(
          name = "dayOfBirth",
          `type` = Some("string"),
          format = Some("date"),
          required = false,
          nullable = Some(true)
        )
        result.filter(r => r.name == "localDateTime").seq.head === GenSwaggerParameter(
          name = "localDateTime",
          `type` = Some("string"),
          format = Some("date-time"),
          required = false,
          nullable = Some(true)
        )
      }

      "with correct array property" >> {
        val itemsParam = GenSwaggerParameter(name = "customKey", required = true, `type` = Some("string"))
        result.filter(r => r.name == "customKey").seq.head === GenSwaggerParameter(
          name = "customKey",
          required = true,
          `type` = Some("array"),
          items = Some(itemsParam)
        )
      }

      "with correct string property" >> {
        result.filter(r => r.name == "firstName").seq.head === GenSwaggerParameter(
          name = "firstName",
          `type` = Some("string"),
          required = false,
          nullable = Some(true)
        )
      }

      "with reference type" >> {
        result.filter(r => r.name == "attribute").seq.head === GenSwaggerParameter(
          name = "attribute",
          referenceType = Some("com.iheart.playSwagger.Attribute"),
          required = false,
          nullable = Some(true)
        )
      }

      "with java collection reference type" >> {
        val itemsParamList =
          GenSwaggerParameter(
            name = "attributeList",
            required = true,
            referenceType = Some("com.iheart.playSwagger.Attribute")
          )
        result.filter(r => r.name == "attributeList").seq.head === GenSwaggerParameter(
          name = "attributeList",
          `type` = Some("array"),
          items = Some(itemsParamList),
          required = false,
          nullable = Some(true)
        )

        val itemsParamSet =
          GenSwaggerParameter(
            name = "attributeSet",
            required = true,
            referenceType = Some("com.iheart.playSwagger.Attribute")
          )
        result.filter(r => r.name == "attributeSet").seq.head === GenSwaggerParameter(
          name = "attributeSet",
          `type` = Some("array"),
          items = Some(itemsParamSet),
          required = false,
          nullable = Some(true)
        )
      }
    }
  }
}
