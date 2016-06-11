package com.iheart.playSwagger

import play.routes.compiler._

trait RouteLiftables extends Liftables {
  import universe._

  // No seq liftable exists, most likely because it's a generic interface and the generated tree would have to choose
  // some implementation. This simply defers to Seq's companion object
  implicit def seqLiftable[A](implicit l: Liftable[A]): Liftable[Seq[A]] =
    Liftable[Seq[A]](f ⇒ q"_root_.scala.collection.Seq(..$f)")

  // Ideally, none of these instances would be required, but PathPart causes us to need one custom instance, and the
  // GenericLiftable implicit resolution hangs indefinitely after that.

  implicit lazy val dynamicPartLiftable = GenericLiftable[DynamicPart]
  implicit lazy val staticPartLiftable = GenericLiftable[StaticPart]

  // PathPart is not a sealed trait, so it's children cannot be found by shapeless, so a custom Liftable is required
  implicit lazy val pathPartLiftable: Liftable[PathPart] = new Liftable[PathPart] {
    def apply(part: PathPart) = part match {
      case d: DynamicPart ⇒ dynamicPartLiftable(d)
      case s: StaticPart  ⇒ staticPartLiftable(s)
    }
  }

  implicit lazy val parameterLiftable = GenericLiftable[Parameter]
  implicit lazy val handlerLiftable = GenericLiftable[HandlerCall]
  implicit lazy val httpVerbLiftable = GenericLiftable[HttpVerb]
  implicit lazy val pathPatternLiftable = GenericLiftable[PathPattern]
  implicit lazy val commentLiftable = GenericLiftable[Comment]
  implicit lazy val routeLiftable = GenericLiftable[Route]

  implicit lazy val routesLiftable = GenericLiftable[Routing]

}
