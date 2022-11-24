package com.iheart.playSwagger

import scala.collection.immutable.SortedMap
import scala.reflect.runtime.universe._
import scala.util.matching.Regex

import ParametricType._

case class ParametricType private (
    tpe: Type,
    reifiedTypeName: String,
    className: String,
    typeArgsMapping: Map[Line, String]
) {
  val resolve: String => String = {
    case ParametricTypeClassName(className, typeArgs) =>
      val resolvedTypes =
        typeArgs
          .split(",")
          .map(_.trim)
          .map(tn => typeArgsMapping.getOrElse(tn, resolve(tn)))
      s"$className[${resolvedTypes.mkString(",")}]"
    case cn => typeArgsMapping.getOrElse(cn, cn)
  }
}

object ParametricType {
  final val ParametricTypeClassName: Regex = "^(.*?)\\[(.*)\\]$".r

  def apply(reifiedTypeName: String)(implicit cl: ClassLoader): ParametricType = {
    val mirror = runtimeMirror(cl)
    reifiedTypeName match {
      case ParametricTypeClassName(className, typeArgsStr) =>
        val sym = mirror.staticClass(className)
        val tpe = sym.selfType
        val typeArgs = typeArgsStr.split(",").map(_.trim).toList
        val typeArgsMapping = SortedMap(tpe.typeArgs.map(_.toString).zip(typeArgs): _*)
        ParametricType(tpe, reifiedTypeName, className, typeArgsMapping)
      case className =>
        val sym = mirror.staticClass(className)
        val tpe = sym.selfType
        ParametricType(tpe, className, className, SortedMap.empty)
    }
  }

  def apply[T: TypeTag]: ParametricType = {
    val tpe = implicitly[TypeTag[T]].tpe
    ParametricType(tpe, tpe.typeSymbol.fullName, tpe.typeSymbol.fullName, Map.empty)
  }
}
