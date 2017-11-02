import sbt.Keys._
import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._
import sbt._

object ScriptedTesting {

  def settings = ScriptedPlugin.scriptedSettings ++ Seq(
    scriptedBufferLog := false,
    scriptedLaunchOpts := scriptedLaunchOpts.value ++ Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value),
    test in Test := (test in Test).dependsOn(scripted.toTask("")).value)
}