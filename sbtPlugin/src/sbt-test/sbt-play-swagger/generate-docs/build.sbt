
logLevel in update := sbt.Level.Warn

enablePlugins(PlayScala, SwaggerPlugin)

name := "app"

scalaVersion := "2.11.7"

swaggerDomainNameSpaces := Seq("namespace1", "namespace2")
