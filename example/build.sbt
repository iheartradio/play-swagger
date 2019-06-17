name := """example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin) //enable plugin

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test,
  "org.webjars" % "swagger-ui" % "2.2.0"  //play-swagger ui integration
)

swaggerDomainNameSpaces := Seq("models")

