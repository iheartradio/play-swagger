package com.iheart.playSwagger

import org.specs2.mutable.Specification

class DomainModelQualifierSpec extends Specification {
  "isModel with multiple packages" >> {

    val dmq = DomainModelQualifier("com.a", "com.b")

    "returns true if the class is in one of the packages" >> {

      dmq.isModel("com.a.foo") must beTrue
    }

    "returns false if the class is not in any of the packages" >> {
      dmq.isModel("com.c.foo") must beFalse
    }
  }

  "isModel with no packages" >> {
    "returns false" >> {
      val dmq = DomainModelQualifier()
      dmq.isModel("com.c.foo") must beFalse
    }
  }
}
