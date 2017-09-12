libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
