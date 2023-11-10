name := """example"""

version := "1.0-SNAPSHOT"

ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0",
  "net.pixiv" %% "scalafix-pixiv-rule" % "4.5.3"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin) //enable plugin

scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  jdbc,
  cacheApi,
  ws,
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
  "org.webjars" % "swagger-ui" % "2.2.0" // play-swagger ui integration
)

scalacOptions ++= Seq("-Xlint:unused")

swaggerDomainNameSpaces := Seq("models")
