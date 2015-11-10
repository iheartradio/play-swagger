package com.iheart.playSwagger

import org.specs2.mutable.Specification

class RoutesFileReaderSpec extends Specification {

  "Reads" should {
    "read all files" in {
      implicit val cl = getClass.getClassLoader
      val results = RoutesFileReader().readAll()
      results.keys must contain(allOf("api", "doc"))
      results("api").size must be_>(2)
      results("doc") must contain("POST    /api/resource/   controllers.Resource.post()")
    }
  }

}
