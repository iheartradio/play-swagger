addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.11")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
