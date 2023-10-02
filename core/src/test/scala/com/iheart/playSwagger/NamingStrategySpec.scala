package com.iheart.playSwagger

import com.iheart.playSwagger.generator.NamingConvention
import org.specs2.mutable.Specification

class NamingStrategySpec extends Specification {
  "naming strategy" >> {
    "none" >> {
      NamingConvention.fromString("none")("attributeName") must be("attributeName")
    }

    "snake_case" >> {
      NamingConvention.fromString("snake_case")("attributeName") must equalTo("attribute_name")
    }

    "snake_case_skip_number" >> {
      NamingConvention.fromString("snake_case_skip_number")("attributeName1") must equalTo("attribute_name1")
    }

    "kebab-case" >> {
      NamingConvention.fromString("kebab-case")("attributeName") must equalTo("attribute-name")
    }

    "lowercase" >> {
      NamingConvention.fromString("lowercase")("attributeName") must equalTo("attributename")
    }

    "UpperCamelCase" >> {
      NamingConvention.fromString("UpperCamelCase")("attributeName") must equalTo("AttributeName")
    }
  }
}
