# Swagger API spec generator for Play 

[![Build Status](https://travis-ci.org/iheartradio/play-swagger.svg)](https://travis-ci.org/iheartradio/play-swagger)
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

#### A simple example

In a `cards.routes` which is referenced in `routes` as 
```
->  /api/cards    cards.Routes
```
You write the following swagger spec in comment. This example is in yml, you can write json if you prefer
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

This will generate the path with summary, tags, parameters and a response with schema defined. It also recursively generates definitions from your case class.  
This works fine if you using a simple `Json.format[CardCreated]` to generate the json response out of this class. If not, you will have to write the definition yourself in the base swagger spec and reference it here at the endpoint. 

The result swagger specs will look like:
  ![](http://amp-public-share.s3-website-us-east-1.amazonaws.com/shifu/play-swagger-sample.png)


#### How to override?
To override any of the automatically generated field, you just need to write the same part in your comment or base swagger sepc file. 

#### How to hide an endpoint?
If you don't want an end point to be included, add `### NoDocs ###` in front of it 
e.g.
```
### NoDocs ###
GET   /docs/swagger-ui/*file        controllers.Assets.at(path:String="/public/lib/swagger-ui", file:String)
```

#### How to specify body content in a post 
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

#### How to find more examples?
In the tests!
/test/scala/com.iheart.playSwagger/SwaggerSpecGeneratorSpec.scala
 
============================
## Installation

In short you need to create a controller that uses the library to generate the swagger spec and make it available as an endpoint.
Then you just need to have a swagger UI instance to consumer that swagger spec. 
  

#### Step 1
add Swagger API dependency to your sbt
```scala
resolvers += Resolver.jcenterRepo

libraryDependencies +=  "com.iheart" %% "play-swagger" % "0.1.2-RELEASE"
```

#### Step 2
add a controller to your Play app that serves the swagger spec

```scala

class ApiSpecs @Inject() (router: Router, cached: Cached) extends Controller {
  implicit val cl = getClass.getClassLoader
  
  // The root package of your domain classes, PlaySwagger will automatically generate definitions when it encounters class references in this package.
  // In our case it would be "com.iheart"
  val domainPackage = "YOUR.DOMAIN.PACKAGE"  
  private lazy val generator = SwaggerSpecGenerator(domainPackage)   

  def specs = cached("swaggerDef") {
    Action { _ ⇒
      Ok(generator.generate(router.documentation))
    }
  }

}
```

#### Step 3
add an end point to the routes file 
```
###
# summary: swagger definition
# description: for swagger UI to consume
###
GET   /docs/swagger.json         @controllers.swagger.ApiSpecs.specs

```

#### Step 4
Add a base swagger.yml (or swagger.json) to your resources folder. This one needs to provide all the required fields according to swagger spec
E.g.
```yml
---
  swagger: "2.0"
  info: 
    title: "Poweramp API"
    description: "Power your music"
    version: "1.0.0"
  host: api2.iheart.com
  schemes:
    - "https"
  consumes:
    - application/json
  produces: 
    - application/json

```

#### Step 5a
Deploy a swagger ui and point to the swagger spec end point, or

#### Step 5b
Alternatively you can use swagger-ui webjar and have you play app serving the swagger ui:

Add the following dependency
```scala
libraryDependencies += "org.webjars" % "swagger-ui" % "2.1.2"
```

Add the following to your route file
```
### NoDocs ###
GET   /docs/swagger-ui/*file        controllers.Assets.at(path:String="/public/lib/swagger-ui", file:String)

```

Then you should be able to open the swagger ui at
http://localhost:9000/docs/swagger-ui/index.html?url=/docs/swagger.json




