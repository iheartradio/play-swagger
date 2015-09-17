# Swagger API spec generator for Play 


## Principles in this generator 

1. No code pollution (e.g. annotation) 
2. DRY (extract as much information from the code as possible)
3. When documenting an endpoint, it should be just swagger specification that you need to write. You shall not need to learn another API or spec format.                 


=======
Which translates to 

1. No annotation needed 
2. No need to learn yet another API, just learn how to write a swagger specification
3. Write your swagger specification in your routes files as comments 
4. Write a base swagger specification for general information that doesn't fit into the routes files.  
5. Reference your case classes in your swagger spec and play-swagger will generate swagger definition for you 


## Installation

### Step 1
add Swagger API dependency to your sbt


### Step 2
add a controller to your Play app that serves the swagger spec

```scala

class ApiSpecs @Inject() (router: Router, cached: Cached) extends Controller {
  implicit val cl = getClass.getClassLoader
  val domainPackage = "YOUR.DOMAIN.PACKAGE"  //the root package of your domain classes, PlaySwagger will automatically generate definitions when it encounters class references in this package.
  private lazy val generator = SwaggerSpecGenerator(domainPackage)   

  def specs = cached("swaggerDef") {
    Action { _ ⇒
      Ok(generator.generate(router.documentation))
    }
  }

}
```

### Step 3
add an end point to the routes file 
```
###
# summary: swagger definition
# description: for swagger UI to consume
###
GET   /docs/swagger.json         @controllers.swagger.ApiSpecs.specs

```

### Step 4
Add a base swagger.yml to your resources folder. This one needs to provide all the required fields according to swagger spec
E.g.
```
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

### Step 5a
Deploy a swagger ui and point to the swagger spec end point.

### Step 5b
Alternatively you can use swagger-ui webjar and have you play app serving the swagger ui:

Add the following dependency
```
libraryDependencies += "org.webjars" % "swagger-ui" % "2.1.2"
```

Add the following to your route file
```
### NoDocs ###
GET   /docs/swagger-ui/*file        controllers.Assets.at(path:String="/public/lib/swagger-ui", file:String)

```

Then you should be able to open the swagger ui at
http://localhost:9000/docs/swagger-ui/index.html?url=/docs/swagger.json




