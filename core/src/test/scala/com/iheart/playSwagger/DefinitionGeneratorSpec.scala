package com.iheart.playSwagger

import com.iheart.playSwagger.Domain._
import org.specs2.mutable.Specification
import play.api.libs.json.Json

case class DictType(key: String, value: String)
case class Foo(barStr: String, barInt: Int, barLong: Option[Long], reffedFoo: ReffedFoo, seqReffedFoo: Seq[ReffedFoo], optionSeqReffedFoo: Option[Seq[ReffedFoo]], dictType: DictType)
case class ReffedFoo(name: String, refrefFoo: RefReffedFoo)
case class RefReffedFoo(bar: String)

case class FooWithSeq(seq: Seq[SeqItem])

case class SeqItem(bar: String)

case class FooWithWrappedStringProperties(required: WrappedString, optional: Option[WrappedString], seq: Seq[WrappedString])
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

object MyObject {
  type MyId = Int
  case class MyInnerClass(bar: String, id: MyId)
}

object ExcludingDomainQualifier extends DomainModelQualifier {
  val parent = PrefixDomainModelQualifier("com.iheart.playSwagger")
  val exclusions = Seq("com.iheart.playSwagger.DictType")
  override def isModel(className: String): Boolean = parent.isModel(className) && !(exclusions contains className)
}

class DefinitionGeneratorSpec extends Specification {
  implicit val cl = getClass.getClassLoader

  "definition" >> {

    "generate name correctly" >> {
      DefinitionGenerator().definition[Foo].name === "com.iheart.playSwagger.Foo"
    }

    "generate from string classname " >> {
      DefinitionGenerator().definition("com.iheart.playSwagger.Foo").name === "com.iheart.playSwagger.Foo"
    }

    "generate properties" >> {

      val result = DefinitionGenerator("com.iheart.playSwagger", Nil, NamingStrategy.None).definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === GenSwaggerParameter(name = "barStr", `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === GenSwaggerParameter(name = "barInt", `type` = Some("integer"), format = Some("int32"))
      }

      "with correct long property" >> {
        result(2) === GenSwaggerParameter(name = "barLong", `type` = Some("integer"), format = Some("int64"), required = false)
      }

      "with reference type" >> {
        result(3) === GenSwaggerParameter(name = "reffedFoo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
      }

      "with sequence of reference type" >> {
        val itemsParam = GenSwaggerParameter(name = "seqReffedFoo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(4) === GenSwaggerParameter(name = "seqReffedFoo", `type` = Some("array"), items = Some(itemsParam))
      }

      "with optional sequence of reference type" >> {
        val itemsParam = GenSwaggerParameter(name = "optionSeqReffedFoo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(5) === GenSwaggerParameter(name = "optionSeqReffedFoo", `type` = Some("array"), items = Some(itemsParam), required = false)
      }

    }

    "generate properties using snake case naming strategy" >> {

      val result = DefinitionGenerator("com.iheart.playSwagger", Nil, NamingStrategy.SnakeCase).definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === GenSwaggerParameter(name = "bar_str", `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === GenSwaggerParameter(name = "bar_int", `type` = Some("integer"), format = Some("int32"))
      }

      "with correct long property" >> {
        result(2) === GenSwaggerParameter(name = "bar_long", `type` = Some("integer"), format = Some("int64"), required = false)
      }

      "with reference type" >> {
        result(3) === GenSwaggerParameter(name = "reffed_foo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
      }

      "with sequence of reference type" >> {
        val itemsParam = GenSwaggerParameter(name = "seq_reffed_foo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(4) === GenSwaggerParameter(name = "seq_reffed_foo", `type` = Some("array"), items = Some(itemsParam))
      }

      "with optional sequence of reference type" >> {
        val itemsParam = GenSwaggerParameter(name = "option_seq_reffed_foo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(5) === GenSwaggerParameter(name = "option_seq_reffed_foo", `type` = Some("array"), items = Some(itemsParam), required = false)
      }

    }

    "generate properties using kebab case naming strategy" >> {

      val result = DefinitionGenerator("com.iheart.playSwagger", Nil, NamingStrategy.KebabCase).definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === GenSwaggerParameter(name = "bar-str", `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === GenSwaggerParameter(name = "bar-int", `type` = Some("integer"), format = Some("int32"))
      }

      "with correct long property" >> {
        result(2) === GenSwaggerParameter(name = "bar-long", `type` = Some("integer"), format = Some("int64"), required = false)
      }

      "with reference type" >> {
        result(3) === GenSwaggerParameter(name = "reffed-foo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
      }

      "with sequence of reference type" >> {
        val itemsParam = GenSwaggerParameter(name = "seq-reffed-foo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(4) === GenSwaggerParameter(name = "seq-reffed-foo", `type` = Some("array"), items = Some(itemsParam))
      }

      "with optional sequence of reference type" >> {
        val itemsParam = GenSwaggerParameter(name = "option-seq-reffed-foo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(5) === GenSwaggerParameter(name = "option-seq-reffed-foo", `type` = Some("array"), items = Some(itemsParam), required = false)
      }

    }

    "read class in Object" >> {
      val result = DefinitionGenerator("com.iheart", Nil, NamingStrategy.None).definition("com.iheart.playSwagger.MyObject.MyInnerClass")
      result.properties.head.name === "bar"
    }

    "read alias type in Object" >> {
      val result = DefinitionGenerator("com.iheart", Nil, NamingStrategy.None).definition("com.iheart.playSwagger.MyObject.MyInnerClass")

      val last = result.properties.last.asInstanceOf[GenSwaggerParameter]
      last.name === "id"
      last.`type` === Some("integer")
      last.referenceType === None
    }

    "read sequence items" >> {
      val result = DefinitionGenerator("com.iheart", Nil, NamingStrategy.None).definition("com.iheart.playSwagger.FooWithSeq")
      result.properties.head.asInstanceOf[GenSwaggerParameter].items.get.asInstanceOf[GenSwaggerParameter].referenceType === Some("com.iheart.playSwagger.SeqItem")
    }

    "read primitive sequence items" >> {
      val result = DefinitionGenerator("com.iheart", Nil, NamingStrategy.None).definition("com.iheart.playSwagger.WithListOfPrimitive")
      result.properties.head.asInstanceOf[GenSwaggerParameter].items.get.asInstanceOf[GenSwaggerParameter].`type` === Some("integer")

    }

    "read Optional items " >> {
      val result = DefinitionGenerator("com.iheart", Nil, NamingStrategy.None).definition("com.iheart.playSwagger.FooWithOption")
      result.properties.head.asInstanceOf[GenSwaggerParameter].referenceType must beSome("com.iheart.playSwagger.OptionItem")
    }

    "with dates" >> {
      "no override" >> {
        val result = DefinitionGenerator("com.iheart", Nil, NamingStrategy.None).definition("com.iheart.playSwagger.WithDate")
        val prop = result.properties.head.asInstanceOf[GenSwaggerParameter]
        prop.`type` must beSome("integer")
        prop.format must beSome("epoch")

      }
      "with override" >> {
        val customJson = List(Json.obj("type" → "string", "format" → "date-time"))
        val mappings = List(
          CustomTypeMapping(
            `type` = "org.joda.time.DateTime",
            specAsParameter = customJson))
        val result = DefinitionGenerator("com.iheart", mappings, NamingStrategy.None).definition("com.iheart.playSwagger.WithDate")
        val prop = result.properties.head.asInstanceOf[CustomSwaggerParameter]
        prop.specAsParameter === customJson
      }
    }

    "with property overrides" >> {
      val customJson = List(Json.obj("type" → "string"))
      val customMapping = CustomTypeMapping(
        `type` = "com.iheart.playSwagger.WrappedString",
        specAsParameter = customJson)
      val generator = DefinitionGenerator("com.iheart", List(customMapping), NamingStrategy.None)
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
    val allDefs = DefinitionGenerator(modelQualifier = ExcludingDomainQualifier).allDefinitions(List("com.iheart.playSwagger.Foo"))
    allDefs.length === 3
    allDefs.find(_.name == "com.iheart.playSwagger.ReffedFoo") must beSome[Definition]
    allDefs.find(_.name == "com.iheart.playSwagger.RefReffedFoo") must beSome[Definition]
    allDefs.find(_.name == "com.iheart.playSwagger.Foo") must beSome[Definition]
  }
}
