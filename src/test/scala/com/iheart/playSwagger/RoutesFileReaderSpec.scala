package com.iheart.playSwagger

import org.specs2.mutable.Specification

class RoutesFileReaderSpec extends Specification {

  "Reads" should {
    "read all files" in {
      implicit val cl = getClass.getClassLoader
      val results = RoutesFileReader().readAll().get
      val fileNames = Seq(
        "test", "routes", "students", "level1", "player", "resource", "level2",
        "customResource", "liveMeta", "no"
      )
      results.keys must contain(allOf(fileNames: _*))
      results("player").size ==== 16
      results("player") must contain("POST     /:pid/playedTracks             controllers.Player.addPlayedTracks(pid)")
    }
  }

}
