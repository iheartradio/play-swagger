package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.{Definition, SwaggerParameter}
import org.specs2.mutable.Specification

case class DictType(key: String, value: String)
case class Foo(barStr: String, barInt: Int, barLong: Option[Long], reffedFoo: ReffedFoo, seqReffedFoo: Seq[ReffedFoo], optionSeqReffedFoo: Option[Seq[ReffedFoo]], dictType: DictType)
case class ReffedFoo(name: String, refrefFoo: RefReffedFoo)
case class RefReffedFoo(bar: String)

case class FooWithSeq(seq: Seq[SeqItem])

case class SeqItem(bar: String)

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

      val result = DefinitionGenerator("com.iheart.playSwagger").definition[Foo].properties

      result.length === 7

      "with correct string property" >> {
        result.head === SwaggerParameter(name = "barStr", `type` = Some("string"))
      }

      "with correct int32 property" >> {
        result(1) === SwaggerParameter(name = "barInt", `type` = Some("integer"), format = Some("int32"))
      }

      "with correct long property" >> {
        result(2) === SwaggerParameter(name = "barLong", `type` = Some("integer"), format = Some("int64"), required = false)
      }

      "with reference type" >> {
        result(3) === SwaggerParameter(name = "reffedFoo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
      }

      "with sequence of reference type" >> {
        val itemsParam = SwaggerParameter(name = "seqReffedFoo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(4) === SwaggerParameter(name = "seqReffedFoo", `type` = Some("array"), items = Some(itemsParam))
      }

      "with optional sequence of reference type" >> {
        val itemsParam = SwaggerParameter(name = "optionSeqReffedFoo", referenceType = Some("com.iheart.playSwagger.ReffedFoo"))
        result(5) === SwaggerParameter(name = "optionSeqReffedFoo", `type` = Some("array"), items = Some(itemsParam), required = false)
      }

    }

    "read class in Object" >> {
      val result = DefinitionGenerator("com.iheart").definition("com.iheart.playSwagger.MyObject.MyInnerClass")
      result.properties.head.name === "bar"
    }

    "read alias type in Object" >> {
      val result = DefinitionGenerator("com.iheart").definition("com.iheart.playSwagger.MyObject.MyInnerClass")
      result.properties.last.name === "id"
      result.properties.last.`type` === Some("integer")
      result.properties.last.referenceType === None
    }

    "read sequence items" >> {
      val result = DefinitionGenerator("com.iheart").definition("com.iheart.playSwagger.FooWithSeq")
      result.properties.head.items.get.referenceType === Some("com.iheart.playSwagger.SeqItem")
    }

    "read primitive sequence items" >> {
      val result = DefinitionGenerator("com.iheart").definition("com.iheart.playSwagger.WithListOfPrimitive")
      result.properties.head.items.get.`type` === Some("integer")

    }

    "read Optional items " >> {
      val result = DefinitionGenerator("com.iheart").definition("com.iheart.playSwagger.FooWithOption")
      result.properties.head.referenceType must beSome("com.iheart.playSwagger.OptionItem")
    }

    "with dates" >> {
      "no override" >> {
        val result = DefinitionGenerator("com.iheart").definition("com.iheart.playSwagger.WithDate")
        val prop = result.properties.head
        prop.`type` must beSome("integer")
        prop.format must beSome("epoch")

      }
      "with override" >> {
        val settings = Settings(Seq(SwaggerMapping("org.joda.time.DateTime", "string", Some("date-time"))))
        val result = DefinitionGenerator("com.iheart", settings).definition("com.iheart.playSwagger.WithDate")
        val prop = result.properties.head
        prop.`type` must beSome("string")
        prop.format must beSome("date-time")
      }
    }
  }

  "allDefinitions" >> {
    val allDefs = DefinitionGenerator(ExcludingDomainQualifier).allDefinitions(List("com.iheart.playSwagger.Foo"))
    allDefs.length === 3
    allDefs.find(_.name == "com.iheart.playSwagger.ReffedFoo") must beSome[Definition]
    allDefs.find(_.name == "com.iheart.playSwagger.RefReffedFoo") must beSome[Definition]
    allDefs.find(_.name == "com.iheart.playSwagger.Foo") must beSome[Definition]
  }
}
