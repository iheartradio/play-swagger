
[![Build Status](https://travis-ci.org/iheartradio/play-swagger.svg)](https://travis-ci.org/iheartradio/play-swagger)
[![Coverage Status](https://coveralls.io/repos/iheartradio/play-swagger/badge.svg?branch=master&service=github)](https://coveralls.io/github/iheartradio/play-swagger?branch=master)
[![Stories in Ready](https://badge.waffle.io/iheartradio/play-swagger.svg?label=ready&title=Ready)](http://waffle.io/iheartradio/play-swagger)
[ ![Download](https://api.bintray.com/packages/iheartradio/maven/play-swagger/images/download.svg) ](https://bintray.com/iheartradio/maven/play-swagger/_latestVersion)
[![Gitter](https://badges.gitter.im/iheartradio/play-swagger.svg)](https://gitter.im/iheartradio/play-swagger?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# Swagger API spec generator for Play

A library that generates swagger specs from route files and case class reflection, no code annotation needed.  
 
## Principles in this lib

1. No code pollution (e.g. annotation) 
2. DRY (extract as much information from the code as possible)
3. When documenting an endpoint, it should be just swagger specification that you need to write. You shall not need to learn another API or spec format.                 

Which translates to 

1. Write your [swagger specification](http://swagger.io/specification/) in your routes files as comments (json or yml)
2. Reference your case classes in your swagger spec and play-swagger will generate definitions
3. Override anything in either the swagger spec in comment or the base swagger spec file (swagger.yml or swagger.json in your conf)

============================
## Day-to-day usage 

For installation/get-started see the next section. 

#### A simple example

In a `cards.routes` which is referenced in `routes` as 
```
->  /api/cards    cards.Routes
```
You can write the following swagger spec in comment (This example is in yml, but json is also supported). The comment has to start and end with `###`.

If you don't write any comment here the endpoint is still going to be picked up by play-swagger, the parameters will be included but there will not be any response format. 
This allows newly added endpoints to be automatically included in swagger with some basic information.      
```
 ###
 #  summary: create a card 
 #  tags:
 #    - Card Endpoints
 #  responses:
 #    200:
 #      description: success
 #      schema:
 #        $ref: '#/definitions/com.iheart.api.Protocol.CardCreated'
 ###
 POST      /users/:profileId/contexts/:contextName/cards       controllers.api.Cards.createCard(profileId: Int, contextName: Option[String])

```

Note that everything in the comment is just standard swagger definition, and it $refs to a case class CardCreated, which is defined in a Protocol object, and it references another case class Card. Here is the source code:
```scala
package com.iheart.api

object Protocol {
  case class CardCreated(card: Card)
  
  case class Card(id: Int, name: String)
}
```

This will generate the path with summary, tags, parameters and a response with schema defined, which comes from the comments and case class reflection. 
It also recursively generates definitions from your case class.  
These schemas assumes that you are using a simple `Json.format[CardCreated]` to generate the json response out of this class.
If not, you will have to write the definition yourself in the base swagger spec and reference it here at the endpoint 
(give it a different name than the full package name though, play-swagger will try to generate definitions for any $ref that starts with the domain package name). 

The result swagger specs will look like:
  ![](http://amp-public-share.s3-website-us-east-1.amazonaws.com/shifu/play-swagger-sample.png)

 
============================
## Get Started

In short you need to add sbt-play-swagger plugin which generates swagger.json on package time,
then you just need to have a swagger UI instance to consumer that swagger spec.
You can find the setup in the example project as well.
  

#### Step 1

For play2.5 add Swagger sbt plugin dependency to your plugins.sbt (see [the releases tab](https://github.com/iheartradio/play-swagger/releases) for the latest versions)

For Play 2.5
```scala
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.6.5")
```

For play 2.6, sbt 1.x and Scala 2.12.x please use 
```scala
addSbtPlugin("com.iheart" %% "sbt-play-swagger" % "0.7.5")
```

For play 2.7, sbt 1.x and Scala 2.12.x please use
```scala
addSbtPlugin("com.iheart" %% "sbt-play-swagger" % "0.7.5-PLAY2.7")
```

For play 2.6 and sbt 0.13.x please use a special release build with play 2.6 binary. (No longer maintained after 0.6.2)
```scala
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.6.2-PLAY2.6")
```

For play 2.4 please use a special release build with play 2.4 binary (No longer maintained after 0.6.0)
```scala
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.6.0-PLAY2.4")

```

Then enable it for your Play app - in build.sbt add `SwaggerPlugin` to the root project like
```Scala
lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin) //enable plugin
```

Also in build.sbt add domain package names for play-swagger to auto generate swagger definitions for domain classes mentioned in your routes
```Scala
swaggerDomainNameSpaces := Seq("models")
```

This plugin adds a sbt task `swagger`, with which you can generate the `swagger.json` for testing purpose.

This plugin will generate the `swagger.json`and make it available under path `assets/swagger.json` on `sbt package` and `sbt run`.

Alternatively, you can create a controller that uses play-swagger lib to generate the json and serve it, this way you can manipulate the swagger.json at runtime. See [here](docs/AlternativeSetup.md) for details.


#### Step 2
Add a base `swagger.yml` (or `swagger.json`) to your `resources` folder (for example, conf folder in the play application). This file needs to provide all the required fields according to swagger spec.

E.g.
```yml
---
  swagger: "2.0"
  info: 
    title: "Poweramp API"
    description: "Power your music"
  consumes:
    - application/json
  produces: 
    - application/json

```
Note that `info.version` is intentionally left out, playSwagger will automatically fillin the build version of the project. However if the version is set here it will be honored. You can also dynamically generate the version string in build file using the `swaggerAPIVersion` setting.   

#### Step 3a
Deploy a swagger ui and point to the swagger spec end point at 'assets/swagger.json', or

#### Step 3b
Alternatively you can use swagger-ui webjar and have you play app serving the swagger ui:

Add the following dependency
```scala
libraryDependencies += "org.webjars" % "swagger-ui" % "2.2.0"
```

Add the following to your route file
```
### NoDocs ###
GET   /docs/swagger-ui/*file        controllers.Assets.at(path:String="/public/lib/swagger-ui", file:String)

### NoDocs ###
GET   /assets/*file                 controllers.Assets.versioned(path="/public", file: Asset)
```

The sbt-play-swagger plugin will generate the swagger.json on `sbt run` or `sbt package`
you should be able to open the swagger ui at
http://localhost:9000/docs/swagger-ui/index.html?url=/assets/swagger.json


============================
## How to contribute

If you have any questions/bug reports, please submit an issue on github. 
With good unit tests coverage, it's pretty easy to add/modify this library as well. 
Code contribution are more than welcome. Make sure that your code is tested and submit the pull request!    


===========================
## FAQ

#### How to override?
To override any of the automatically generated field, you just need to write the same part in your comment or base swagger spec file. 

#### How to override type mappings?
To override the type mapping used for any type, create a swagger-custom-mappings.yml or swagger-custom-mappings.json in conf and add
an array of mappings. Each mapping consists of

1. `type` a regex matching the type for which the custom specs is
1. `specAsParameter` a list of objects to be used when this type is used by a route path parameter or query string parameter. Being a list of json object allows you to expand a single parameter into multiple ones, but in other cases you just need to provide one json object. If you leave this one an empty array, the parameter with this type will be hidden. You must provide a list here, though.
1. `specAsProperty`, a json object to be used when the type is used as a property in a definition. If you leave this one empty, play-swagger will try to use the first element in `specAsParameter`, please note that although most of the fields are common between the two types of spec, a couple of them aren't.

For example
```yaml
---
  - type: java\.time\.LocalDate
    specAsParameter:
      - type: string
        format: date
  - type: java\.time\.Duration
    specAsParameter: []   #this hides the type from query and path parameter
    specAsProperty:
      $ref: "#/definitions/Duration"


```

The preceding example would result in output for a field with type `java.time.LocalDate` like this:
   
```json
"fieldName": {
   "type":"string",
   "format":"date"
}
```

#### The spec is missing when built to a docker image using sbt-native-pakcager

@mosche answered this one in #114
> It's a bit unfortunate the way the docker plugin redefines stage. 
However, the solution is pretty simple. Just add:
```Scala
(stage in Docker) <<= (stage in Docker).dependsOn(swagger)
```

#### How to hide an endpoint?
If you don't want an end point to be included, add `### NoDocs ###` in front of it 
e.g.
```
### NoDocs ###
GET   /docs/swagger-ui/*file        controllers.Assets.at(path:String="/public/lib/swagger-ui", file:String)
```

#### How to specify body content in a POST endpoint 
Body content is specified as a special parameter in swagger. So you need to create a parameter in your swagger spec comment as "body", for example
```
###  
#  parameters:
#    - name: body
#      schema:
#        $ref: '#/definitions/com.iheart.api.Track'
###
POST   /tracks       controller.Api.createTrack()
```
Again, play-swagger will generate the definition for com.iheart.api.Track case class

#### How do I use a different "host" for different environment?
Use the [alternative setup](docs/AlternativeSetup.md). The library returns play JsObject, you can change however you want like
```scala
val spec: Try[JsObject] = ps.generate().map(_ + ("host" -> JsString(myHost)))
```


#### How to use a route file different from the default "routes"?
In build.sbt, add
```Scala
swaggerRoutesFile := "my-routes"
```

or if you took the [alternative setup](docs/AlternativeSetup.md)
```scala
SwaggerSpecGenerator(domainPackage).generate("myRoutes.routes")
```

#### How do I change the location of the swagger documentation in the packaged app?
In build.sbt, add
```scala
swaggerTarget := new File("path/to/swagger/location")
```

#### How do I change the filename of the swagger documentation in the packaged app?
In build.sbt, add
```scala
swaggerFileName := "customSwagger.json"
```

#### How to output formatted json in swagger documentation file?
In build.sbt, add
```scala
swaggerPrettyJson := true
```

#### Where to find more examples?
In the [tests](/core/src/test/scala/com/iheart/playSwagger/SwaggerSpecGeneratorSpec.scala)!


#### How to use markup in swagger specs
You can use markup on your swagger specs by providing `OutputTransformers` classes name to the setting `swaggerOutputTransformers` on your build file.

For example you can use environment variables by adding the configuration:
```
    swaggerOutputTransformers := Seq(envOutputTransformer)
```

Then on your routes file or root swagger file you can use some markup like the one used below for the host field:
```
  swagger: "2.0"
  info:
    title: "API"
    description: "REST API"
  host: ${API_HOST}
```

This way when the swagger file is parsed the markup `${API_HOST}` is going to be substituted by the content of the environent variable `API_HOST`.

#### How to support OpenAPI/Swagger v3
You can produce swagger files for v3 by setting the flag:
```
  swaggerV3 := true
```

Make sure you also update your swagger markup to specify that you are using OpenAPI v3:
```
  openapi: 3.0.0
  info:
    title: "API"
    description: "REST API"
    version: "1.0.0"
```

Also, for `$ref` fields you will want to prefix paths with `#/components/schemas/` instead of `#/definitions/`. For example:

```
###  
#  parameters:
#    - name: body
#      schema:
#        $ref: '#/components/schemas/com.iheart.api.Track'
###
POST   /tracks       controller.Api.createTrack()
```


#### Is play java supported? 

Mostly. Auto definition generation for classes only works with Java classes that have all the fields in the first constructor. 
