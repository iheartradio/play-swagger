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
    def getMethodParamDescription(call: HandlerCall, paramList: Seq[Parameter], param: Parameter): Option[String]
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

    override def getMethodParamDescription(call: HandlerCall, paramList: Seq[Parameter], param: Parameter): Option[String] = {
      val method = Seq(
        call.packageName,
        call.controller,
        call.method).mkString(".")
      val params = paramList.map(_.typeName).mkString(",")
      val key = s"$method($params)#${param.name}"
      map.get(key)
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

    override def getMethodParamDescription(call: HandlerCall, paramList: Seq[Parameter], param: Parameter): Option[String] = None
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
