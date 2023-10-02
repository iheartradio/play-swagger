package com.iheart.playSwagger.util

import play.api.libs.json._

object ExtendJsValue {

  implicit class JsObjectUpdate(jsObject: JsObject) {
    def update(target: String)(f: JsValue => JsObject): collection.Seq[(String, JsValue)] =
      jsObject.fields.flatMap {
        case (k, v) if k == target => f(v).fields
        case (k, v) => Seq(k -> v.update(target)(f))
      }
  }

  implicit class JsValueUpdate(jsValue: JsValue) {
    def update(target: String)(f: JsValue => JsObject): JsValue = jsValue.result match {
      case JsDefined(obj: JsObject) => JsObject(obj.update(target)(f))

      case JsDefined(arr: JsArray) =>
        JsArray(arr.value.map(_.update(target)(f)))

      case JsDefined(js) => js

      case _ => JsNull
    }
  }

  implicit class PathAdditions(path: JsPath) {
    def writeNullableIterable[A <: Iterable[_]](implicit writes: Writes[A]): Writes[A] =
      Writes[A] { (a: A) =>
        if (a.isEmpty) Json.obj()
        else JsPath.createObj(path -> writes.writes(a))
      }
  }

}
