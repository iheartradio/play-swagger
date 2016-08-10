logLevel in update := sbt.Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.8")

{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("com.iheart" % "sbt-play-swagger" % pluginVersion)
}
