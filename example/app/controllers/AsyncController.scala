package controllers

import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

import akka.actor.ActorSystem
import models.Message
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._

/**
  * This controller creates an `Action` that demonstrates how to write
  * simple asynchronous code in a controller. It uses a timer to
  * asynchronously delay sending a response for 1 second.
  *
  * @param actorSystem We need the `ActorSystem`'s `Scheduler` to
  * run code after a delay.
  * @param exec We need an `ExecutionContext` to execute our
  * asynchronous code.
  */
@Singleton
class AsyncController @Inject() (actorSystem: ActorSystem, components: ControllerComponents)(implicit
exec: ExecutionContext) extends AbstractController(components) {
  implicit val fmt: OFormat[Message] = Json.format[Message]

  /**
    * Create an Action that returns a plain text message after a delay
    * of 1 second.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/message`.
    */
  def message: Action[AnyContent] = Action.async {
    getFutureMessage(1.second).map { msg ⇒ Ok(Json.toJson(msg)) }
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[Message] = {
    val promise: Promise[Message] = Promise[Message]()
    actorSystem.scheduler.scheduleOnce(delayTime) { promise.success(Message("Hi!")) }
    promise.future
  }

}
