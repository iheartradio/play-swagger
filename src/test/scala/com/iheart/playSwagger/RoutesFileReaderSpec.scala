package com.iheart.playSwagger

import org.specs2.mutable.Specification

class RoutesFileReaderSpec extends Specification {

  "Reads" should {
    "read all files" in {
      implicit val cl = getClass.getClassLoader
      val results = RoutesFileReader().readAll()
      results.keys must contain(allOf("api", "doc"))
      results.get("api").get.size must be_>(2)
      results.get("doc").get must contain("POST    /api/resource/   controllers.Resource.post()")
    }
  }

}
