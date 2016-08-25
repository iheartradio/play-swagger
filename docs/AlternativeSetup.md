
In short you need to create a controller that uses the library to generate the swagger spec and make it available as an endpoint.

#### Step 1
For play 2.5 add Swagger API dependency to your sbt
```scala
resolvers += Resolver.jcenterRepo

libraryDependencies +=  "com.iheart" %% "play-swagger" % "0.4.0"  //find the latest version in the download badge at the top
```

For play 2.4 please use a special release build with play 2.4 binary.
```scala
libraryDependencies +=  "com.iheart" %% "play-swagger" % "0.4.0-PLAY2.4"  //find the latest version in the download badge at the top
```

#### Step 2
Add a controller that uses  Play swagger as a library to generates a swagger spec json and serves it as an endpoint.

```scala
import play.api.libs.concurrent.Execution.Implicits._
import com.iheart.playSwagger.SwaggerSpecGenerator

class ApiSpecs @Inject() (cached: Cached) extends Controller {
  implicit val cl = getClass.getClassLoader
  
  // The root package of your domain classes, play-swagger will automatically generate definitions when it encounters class references in this package.
  // In our case it would be "com.iheart", play-swagger supports multiple domain package names
  val domainPackage = "YOUR.DOMAIN.PACKAGE"  
  val secondDomainPackage = "YOUR.OtherDOMAIN.PACKAGE"
  private lazy val generator = SwaggerSpecGenerator(domainPackage, secondDomainPackage)
  
  def specs = cached("swaggerDef") {  //it would be beneficial to cache this endpoint as we do here, but it's not required if you don't expect much traffic.   
     Action.async { _ =>
        Future.fromTry(generator.generate()).map(Ok(_)) //generate() can also taking in an optional arg of the route file name. 
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
GET   /assets/swagger.json         @controllers.swagger.ApiSpecs.specs

```


Then follow Step 2 through Step 3 in the sbt-play-swagger setup in the main README.md
