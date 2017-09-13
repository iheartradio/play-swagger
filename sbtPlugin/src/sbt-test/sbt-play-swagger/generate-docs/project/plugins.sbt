logLevel in update := sbt.Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.6")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.14")

{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("com.iheart" % "sbt-play-swagger" % pluginVersion)
}

libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"
