package com.iheart.playSwagger

import com.iheart.playSwagger.Descriptions.DescriptionProviderImpl
import org.specs2.mutable.Specification
import play.routes.compiler.{HandlerCall, Parameter}

class DescriptionsSpec extends Specification {

  val descriptionMap = Map(
    "com.example.FooController.foo(Date)#date" -> "date document 1",
    "com.example.FooController.bar(Date)#date" -> "date document 2",
    "com.example.FooController.bar(Option[Date])#date" -> "date document 3")
  val descriptionProvider = new DescriptionProviderImpl(descriptionMap)
  "DescriptionProviderImpl" >> {
    "getMethodParamDescription should use parameterList from HandlerCall." >> {
      val p1 = Parameter("date", "Date", None, None)
      val hc = new HandlerCall(
        "com.example", "FooController", true, "foo", Some(Seq(
          p1)))
      val func = descriptionProvider.getMethodParameterDescriptionProvider(hc)
      func(p1) === Some("date document 1")
    }
    "getMethodParamDescription should remove package from parameters" >> {
      val p1 = Parameter("date", "java.time.Date", None, None)
      val hc1 = new HandlerCall(
        "com.example", "FooController", true, "bar", Some(Seq(
          p1)))
      val func1 = descriptionProvider.getMethodParameterDescriptionProvider(hc1)
      func1(p1) === Some("date document 2")

      val p2 = Parameter("date", "Option[java.time.Date]", None, None)
      val hc2 = new HandlerCall(
        "com.example", "FooController", true, "bar", Some(Seq(
          p2)))
      val func2 = descriptionProvider.getMethodParameterDescriptionProvider(hc2)
      func2(p2) === Some("date document 3")
    }
  }
}
