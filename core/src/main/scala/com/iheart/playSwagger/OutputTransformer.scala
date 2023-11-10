package com.iheart.playSwagger

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

import com.iheart.playSwagger.OutputTransformer.SimpleOutputTransformer
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

/** Specialization of a Kleisli function (A => M[B]) */
trait OutputTransformer extends (JsObject => Try[JsObject]) {

  /** alias for `andThen` as defined monadic function */
  def >=>(b: JsObject => Try[JsObject]): OutputTransformer = SimpleOutputTransformer { value: JsObject =>
    this.apply(value).flatMap(b)
  }
}

object OutputTransformer {
  final case class SimpleOutputTransformer(run: JsObject => Try[JsObject]) extends OutputTransformer {
    override def apply(value: JsObject): Try[JsObject] = run(value)
  }

  def traverseTransformer(vals: JsArray)(transformer: JsValue => Try[JsValue]): Try[JsArray] = {
    val tryElements = vals.value.map {
      case value: JsObject => traverseTransformer(value)(transformer)
      case value: JsArray => traverseTransformer(value)(transformer)
      case value: JsValue => transformer(value)
    }.toList

    val failures: List[Failure[JsValue]] =
      tryElements.filter(_.isInstanceOf[Failure[_]]).map(_.asInstanceOf[Failure[JsValue]])
    if (failures.nonEmpty) {
      Failure(failures.head.exception)
    } else {
      Success(JsArray(tryElements.asInstanceOf[List[Success[JsValue]]].map(_.value)))
    }
  }

  def traverseTransformer(obj: JsObject)(transformer: JsValue => Try[JsValue]): Try[JsObject] = {
    val tryFields = obj.fields.map {
      case (key, value: JsObject) => (key, traverseTransformer(value)(transformer))
      case (key, values: JsArray) => (key, traverseTransformer(values)(transformer))
      case (key, value: JsValue) => (key, transformer(value))
    }
    val failures: Seq[(String, Failure[JsValue])] = tryFields
      .filter(_._2.isInstanceOf[Failure[_]])
      .asInstanceOf[Seq[(String, Failure[JsValue])]]
    if (failures.nonEmpty) {
      Failure(failures.head._2.exception)
    } else {
      Success(JsObject(tryFields.asInstanceOf[Seq[(String, Success[JsValue])]].map {
        case (key, Success(result)) => (key, result)
      }))
    }
  }
}

class PlaceholderVariablesTransformer(map: String => Option[String], pattern: Regex = ("^\\$\\{(.*)\\}$").r)
    extends OutputTransformer {
  def apply(value: JsObject): Try[JsObject] = OutputTransformer.traverseTransformer(value) {
    case JsString(pattern(key)) => map(key) match {
        case Some(result) => Success(JsString(result))
        case None => Failure(new IllegalStateException(s"Unable to find variable $key"))
      }
    case e: JsValue => Success(e)
  }
}

final case class MapVariablesTransformer(map: Map[String, String]) extends PlaceholderVariablesTransformer(map.get)
class EnvironmentVariablesTransformer extends PlaceholderVariablesTransformer((key: String) =>
      Option(System.getenv(key))
    )

class ParametricTypeNamesTransformer extends OutputTransformer {
  override def apply(obj: JsObject): Try[JsObject] = Success(tf(obj))

  private def tf(obj: JsObject): JsObject = JsObject {
    obj.fields.map {
      case (key, value: JsObject) => (normalize(key), tf(value))
      case (key, JsString(value)) => (normalize(key), JsString(normalize(value)))
      case (key, other) => (normalize(key), other)
      case e => e
    }
  }

  private final val normalize: String => String = {
    case ParametricType.ParametricTypeClassName(className, argsGroup) =>
      val normalizedArgs =
        argsGroup
          .split(",")
          .iterator
          .map(_.trim)
          .map(normalize)
          .mkString("_")
      s"$className-$normalizedArgs"
    case n => n
  }
}
