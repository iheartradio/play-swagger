
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
For play2.5 add Swagger sbt plugin dependency to your plugins.sbt

```scala
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.4.2")
```

For play 2.4 please use a special release build with play 2.4 binary.
```scala
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.4.2-PLAY2.4")

```
Then enable it for your Play app - in build.sbt add `SwaggerPlugin` to the root project like
```Scala
lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin) //enable plugin
```
This adds a sbt task `swagger`, with which you can generate the `swagger.json` for testing purpose.

This plugin will generate the `swagger.json` in the packaged `public` folder for you on `sbt package`, which will make it available under path `assets/swagger.json`

#### Step 2
Add a base swagger.yml (or swagger.json) to your resources (for example, conf folder in the play application). This one needs to provide all the required fields according to swagger spec.

E.g.
```yml
---
  swagger: "2.0"
  info: 
    title: "Poweramp API"
    description: "Power your music"
    version: "1.0.0"
  consumes:
    - application/json
  produces: 
    - application/json

```

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

```

The sbt-play-swagger plugin will generate the swagger.json on packaging, so if you run `sbt start`,
you should be able to open the swagger ui at
http://localhost:9000/docs/swagger-ui/index.html?url=/assets/swagger.json

Note that `sbt run` doesn't generate the swagger.json for you.

Alternatively, you can create a controller that uses play-swagger lib to generate the json and serve it. See [here](docs/AlternativeSetup.md) for details

============================
## How to contribute

If you have any questions/bug reports, please submit an issue on github. 
With good unit tests coverage, it's pretty easy to add/modify this library as well. 
Code contribution are more than welcome. Make sure that your code is tested and submit the pull request!    


===========================
## FAQ

#### How to override?
To override any of the automatically generated field, you just need to write the same part in your comment or base swagger spec file. 

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
The library returns play JsObject, you can change however you want like 
```scala
val spec: Try[JsObject] = ps.generate().map(_ + ("host" -> JsString(myHost)))
```


#### How to use a route file different from the default "routes"?
```scala
SwaggerSpecGenerator(domainPackage).generate("myRoutes.routes")
```

#### Where to find more examples?
In the [tests](/src/test/scala/com/iheart/playSwagger/SwaggerSpecGeneratorSpec.scala)!



