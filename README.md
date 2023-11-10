[![Scala CI](https://github.com/play-swagger/play-swagger/actions/workflows/scala.yml/badge.svg)](https://github.com/iheartradio/play-swagger/actions/workflows/scala.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/play-swagger/sbt-play-swagger/badge.svg)](https://maven-badges.herokuapp.com/maven-central/play-swagger/sbt-play-swagger)

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

For play 2.8, Scala 2.13.x and Scala 2.12.x please use
```scala
addSbtPlugin("io.github.play-swagger" % "sbt-play-swagger" % "1.4.4")
```

Then enable it for your Play app - in build.sbt add `SwaggerPlugin` to the root project like
```Scala
lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin) //enable plugin
```

Also in build.sbt add domain package names for play-swagger to auto generate swagger definitions for domain classes mentioned in your routes
```Scala
swaggerDomainNameSpaces := Seq("models")
```

To be more specific, If you want to use the case class defined in the package `something.models` in swagger (accessed via `#ref:definitions/`), add the following in sbt.
```Scala
swaggerDomainNameSpaces := Seq("something.models")
```

Additionally, if you want to use other packages (e.g. `other.models`), you can do so like this.
```Scala
swaggerDomainNameSpaces := Seq("something.models","other.models")
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

The query parameter `url` is disabled in 4.1.3 and later versions. ([GHSA-qrmm-w75w-3wpx](https://github.com/swagger-api/swagger-ui/security/advisories/GHSA-qrmm-w75w-3wpx))
```scala
libraryDependencies += "org.webjars" % "swagger-ui" % "4.11.1"
```

Copy the `index.html` and `swagger-initializer.js` generated in `target/${project}/public/lib/main/swagger-ui/` and modify the js files as follows to create Swagger-UI can be used easily.
```js
window.onload = function() {
  window.ui = SwaggerUIBundle({
    // edit url
    url: "/assets/swagger.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });
};
```

For more information: [installation.md](https://github.com/swagger-api/swagger-ui/blob/master/docs/usage/installation.md)

The sbt-play-swagger plugin will generate the swagger.json on `sbt run` or `sbt package`.

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

#### How to use a custom naming strategy?

To use a custom naming strategies to override your case classes field names, you need to add this to your `build.sbt`:

```scala
//default is 'none', which is your camelCased case class
swaggerNamingStrategy := "snake_case" //snake_case_skip_number, kebab-case, lowercase and UpperCamelCase also available
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

##### Skip entire file

The entire file can be skipped by adding `### SkipFileForDocs ###` at the beginning of the routes file.

Alternatively, the routes file can be split into multiple files, so that you can skip practically only a part of the file.

https://www.playframework.com/documentation/ja/2.4.x/SBTSubProjects

```
### SkipFileForDocs ###

GET      /api/hidden/a                 controllers.hiddenEndPointA()
GET      /api/hidden/b                 controllers.hiddenEndPointB()
GET      /api/hidden/c                 controllers.hiddenEndPointC()
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

#### Support for generic types in schemas
Generic types in schema definitions for request/response body is supported. Example:
```scala
package models

case class Foo[T](payload: T)
case class AnotherOne(someString: String)
```
One can, then, reference the schema directly with `models.Foo[models.AnotherOne]` 
and a correct OpenAPI 3 spec will be generated (not tested with Swagger 2.0):
```yaml
###
#   summary: Get a message
#   responses:
#       200:
#           description: success
#           content:
#               application/json:
#                   schema:
#                       $ref: '#/components/schemas/models.Foo[models.AnotherOne]'
###
GET     /message        controllers.AsyncController.parametric
```
The generated schema name, however, cannot contain `[`, `]` or `,` which appear in type argument lists in Scala. 
Therefore, there's a default `OutputTransformer` (`ParametricTypeNamesTransformerSpec`) which normalises the name into the URL-compliant form. 
The definitions output would then look like:
```json
{
  "components" : {
    "schemas" : {
      "models.AnotherOne" : {
        "properties" : {
          "someString" : {
            "type" : "string"
          }
        },
        "required" : [ "someString" ]
      },
      "models.Foo-models.AnotherOne" : {
        "properties" : {
          "payload" : {
            "$ref" : "#/components/schemas/models.AnotherOne"
          }
        },
        "required" : [ "payload" ]
      }
    }
  },
...
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/models.Foo-models.AnotherOne"
                }
              }
            }
...
}
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

#### No #definitions generated when referencing other Swagger files

By placing a json or YAML file in `conf/${dir}/${file}` and referencing it with `$ref` in a comment, the file can be generated embedded in swagger.json.

⚠️ **Warning**: If a file that does not exist in `/conf` is specified, or if a typo is used for the filename, `$ref:"${filename}"` will be output as is.

example `conf/routes` file.

```
###
#  summary: Top Page
#  responses:
#    200:
#      $ref: './swagger/home_200.yml'
###
GET     /            controllers.HomeController.index
```

example `conf/swagger/home_200.yml` file.

```yaml
description: "success"
```

Of course, writing `schema` etc. will also be embedded.

Generated `swagger.json`.

```json
{
  "paths": {
    "/": {
      "get": {
        "operationId": "index",
        "tags": [
          "routes"
        ],
        "summary": "Top Page",
        "responses": {
          "200": {
            "description": "success"
          }
        }
      }
    }
  }
  ......
}
```

See the following document for information on how to refer to other files by "$ref".

https://swagger.io/docs/specification/using-ref/

##### You can also cut out the entire comment.

This feature is very useful, but OpenAPI does not allow top-level `$ref`, so failing to embed it may result in an invalid `swagger.json`!

```
###
#  $ref: './swagger/home.yml'
###
GET     /            controllers.HomeController.index
```

example `home.yml` file.

```yaml
summary: Top Page
  responses:
    200:
      description: "success"
```

#### Duplicate operationId?

It can be configured in `build.sbt`. 
This setting allows you to set the `${controllerName}.${methodName}` to name the operationId.

```sbt
swaggerOperationIdNamingFully := true
```

#### Need a schema description?

Using [runtime-scaladoc-reader](https://github.com/takezoe/runtime-scaladoc-reader), a description can be generated from Scaladoc comments written in the case class.

⚠️ Schema generation from documentation comments is very useful, but **should never be used** if the scope of scaladoc documentation is different from the scope of OpenAPI documentation.

Add the required dependencies and Compiler Plugin to `build.sbt` and configure it for use.

```sbt
embedScaladoc := true
addCompilerPlugin("com.github.takezoe" %% "runtime-scaladoc-reader" % "1.0.3")
libraryDependencies +=  "com.github.takezoe" %% "runtime-scaladoc-reader" % "1.0.3"
```

For example, a case class might be written as follows.

```scala
/**
  * @param name e.g. Sunday, Monday, TuesDay...
  */
case class DayOfWeek(name: String)
```

The generated JSON will look like this.

```json
{
  "DayOfWeek": {
    "properties": {
      "name": {
        "type": "string",
        "description": "e.g. Sunday, Monday, TuesDay..."
      }
    },
    "required": [
      "name"
    ]
  }
}
```

#### Is play java supported? 

you can generate models definition from java POJO by setting the flag:
```
  playJava := true
```
The flag only suport PlayJava 2.7 and 2.8
