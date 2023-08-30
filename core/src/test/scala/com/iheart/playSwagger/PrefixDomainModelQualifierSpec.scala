package com.iheart.playSwagger

import com.iheart.playSwagger.generator.PrefixDomainModelQualifier
import org.specs2.mutable.Specification

class PrefixDomainModelQualifierSpec extends Specification {
  "isModel with multiple packages" >> {

    val dmq = PrefixDomainModelQualifier("com.a", "com.b")

    "returns true if the class is in one of the packages" >> {

      dmq.isModel("com.a.foo") must beTrue
    }

    "returns false if the class is not in any of the packages" >> {
      dmq.isModel("com.c.foo") must beFalse
    }
  }

  "isModel with no packages" >> {
    "returns false" >> {
      val dmq = PrefixDomainModelQualifier()
      dmq.isModel("com.c.foo") must beFalse
    }
  }
}
