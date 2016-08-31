package com.iheart.playSwagger

import org.specs2.mutable.Specification

class SwaggerSpecRunnerSpec extends Specification {
  "Runner" >> {
    "parseJson" >> {
      val mappings = SwaggerSpecRunner.getMappings(
        "[ { \"fromType\": \"java.time.LocalDate\", \"toType\": \"string\", \"format\": \"date\" }, " +
          "{ \"fromType\": \"java.time.Duration\", \"toType\": \"integer\" } ]"
      )

      "first mapping" >> {
        mappings.head.fromType mustEqual "java.time.LocalDate"
        mappings.head.toType mustEqual "string"
        mappings.head.format mustEqual Some("date")
      }
      "second mapping" >> {
        mappings(1).fromType mustEqual "java.time.Duration"
        mappings(1).toType mustEqual "integer"
        mappings(1).format mustEqual None
      }
    }
  }
}
