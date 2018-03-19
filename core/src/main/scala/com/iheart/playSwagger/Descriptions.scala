package com.iheart.playSwagger

import java.io.{ File, FileReader }
import java.util.Properties

import play.routes.compiler.{ HandlerCall, Parameter }

import scala.reflect.runtime.universe
import scala.collection.JavaConverters._

object Descriptions {

  trait DescriptionProvider {
    def getTypeDescription(sym: universe.Symbol): Option[String]

    def getParamDescription(sym: universe.Symbol, param: String): Option[String]

    def getMethodParameterDescriptionProvider(call: HandlerCall): Parameter ⇒ Option[String]
  }

  /**
   * Option[java.time.Date] => ["Option", "[", "java.time.Date", "]"]
   *
   * @param all
   * @param typeName
   * @return
   */
  def splitTypeName(all: Seq[String], typeName: String): Seq[String] = {
    if (typeName.isEmpty) {
      all
    } else {
      val part: String = typeName.head match {
        case '[' | ']' ⇒
          typeName.takeWhile(ch ⇒ ch == '[' || ch == ']')
        case _ ⇒
          typeName.takeWhile(ch ⇒ ch != '[' && ch != ']')
      }
      splitTypeName(all :+ part, typeName.substring(part.length))
    }
  }

  class DescriptionProviderImpl(private val map: Map[String, String]) extends DescriptionProvider {
    def getTypeDescription(sym: universe.Symbol): Option[String] = {
      val key = sym.fullName
      map.get(key)
    }

    override def getParamDescription(sym: universe.Symbol, param: String): Option[String] = {
      val key = s"${sym.fullName}.$param"
      map.get(key)
    }

    override def getMethodParameterDescriptionProvider(call: HandlerCall): Parameter ⇒ Option[String] = {
      val paramList = call.parameters.getOrElse(Nil)
      val method = Seq(
        call.packageName,
        call.controller,
        call.method).mkString(".")
      val params = paramList.map(p ⇒ {
        (splitTypeName(Nil, p.typeName) map { typeName ⇒
          typeName.lastIndexOf(".") match {
            case -1 ⇒ typeName
            case n  ⇒ typeName.takeRight(typeName.length - n - 1)
          }
        }).mkString
      }).mkString(",")
      val methodKey = s"$method($params)"

      def _provide(p: Parameter): Option[String] = map.get(s"$methodKey#${p.name}")
      _provide
    }
  }

  object DescriptionProviderImpl {
    /**
     * Initialize description provider with a properties file.
     *
     * @param file
     * @return
     */
    def createFromFile(file: File) = {
      val props = new Properties()
      props.load(new FileReader(file))

      val map = props.stringPropertyNames().asScala.map { name ⇒
        name -> props.getProperty(name)
      }.toMap

      new DescriptionProviderImpl(map)
    }
  }

  /**
   * A dummy implementation to be used when no description file is supplied.
   */
  class EmptyDescriptionProviderImpl extends DescriptionProvider {
    override def getTypeDescription(sym: universe.Symbol): Option[String] = None

    override def getParamDescription(sym: universe.Symbol, param: String): Option[String] = None

    override def getMethodParameterDescriptionProvider(call: HandlerCall): Parameter ⇒ Option[String] =
      _ ⇒ None
  }

  final val Empty = new EmptyDescriptionProviderImpl

  private var _currentProvider: DescriptionProvider = Empty

  def getProvider: DescriptionProvider = _currentProvider

  def useDescriptionFile(file: File) = {
    if (file != null && file.exists() && file.isFile) {
      _currentProvider = DescriptionProviderImpl.createFromFile(file)
    }
  }

}
