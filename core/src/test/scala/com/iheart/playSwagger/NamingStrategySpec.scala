package com.iheart.playSwagger


import org.specs2.mutable.Specification

class NamingStrategySpec extends Specification {
  "naming strategy" >> {
    "none" >> {
      NamingStrategy.from("none")("attributeName") must be("attributeName")
    }

    "snake_case" >> {
      NamingStrategy.from("snake_case")("attributeName") must equalTo("attribute_name")
    }

    "kebab-case" >> {
      NamingStrategy.from("kebab-case")("attributeName") must equalTo("attribute-name")
    }

    "lowercase" >> {
      NamingStrategy.from("lowercase")("attributeName") must equalTo("attributename")
    }

    "UpperCamelCase" >> {
      NamingStrategy.from("UpperCamelCase")("attributeName") must equalTo("AttributeName")
    }
  }
}