
In short you need to create a controller that uses the library to generate the swagger spec and make it available as an endpoint.

#### Step 1
Follow the [Step 1](../README.md#step-1) from the main README.

Note:
  It is sufficient to only add Play swagger as a library dependency in your `build.sbt` rather than a plugin in this setup.
  You'll need to add following dependency:
```scala
  "io.github.play-swagger" %% "play-swagger" % "1.4.4"
```  

#### Step 2
Add a controller that uses  Play swagger as a library to generates a swagger spec json and serves it as an endpoint.

Example (compile time DI):
```scala
package controllers.swagger

import play.api.Configuration
import com.iheart.playSwagger.generator.SwaggerSpecGenerator
import play.api.libs.json.JsString
import play.api.mvc._

class ApiSpecs(cc: ControllerComponents, config: Configuration) extends AbstractController(cc) {
  implicit val cl = getClass.getClassLoader

  val domainPackage = "YOUR.DOMAIN.PACKAGE"
  val otherDomainPackage = "YOUR.OtherDOMAIN.PACKAGE"
  lazy val generator = SwaggerSpecGenerator(domainPackage, otherDomainPackage)

  // Get's host configuration.
  val host = config.get[String]("swagger.host")

  lazy val swagger = Action { request =>
    generator.generate().map(_ + ("host" -> JsString(host))).fold(
      e => InternalServerError("Couldn't generate swagger."),
      s => Ok(s))
  }

  def specs = swagger
}
```

#### Step 3
Add endpoints to the routes file.
```
### NoDocs ###
GET   /docs/swagger-ui/*file        controllers.Assets.at(path:String="/public/lib/swagger-ui", file:String)

### NoDocs ###
GET   /assets/swagger.json          controllers.swagger.ApiSpecs.specs
```

#### Step 4

Follow [Step 2 through Step 3](../README.md#step-2) in the main README.
