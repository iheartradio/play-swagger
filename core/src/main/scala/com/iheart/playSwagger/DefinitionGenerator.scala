package com.iheart.playSwagger

import com.fasterxml.jackson.databind.{ BeanDescription, ObjectMapper }
import com.iheart.playSwagger.Domain.{ CustomMappings, Definition, GenSwaggerParameter, SwaggerParameter }
import com.iheart.playSwagger.SwaggerParameterMapper.mapParam
import com.sun.istack.internal.Nullable
import play.routes.compiler.Parameter

import scala.collection.JavaConverters
import scala.reflect.runtime.universe._

final case class DefinitionGenerator(
  playJava:       Boolean              = false,
  modelQualifier: DomainModelQualifier = PrefixDomainModelQualifier(),
  mappings:       CustomMappings       = Nil)(implicit cl: ClassLoader) {

  def dealiasParams(t: Type): Type = {
    appliedType(t.dealias.typeConstructor, t.typeArgs.map { arg ⇒
      dealiasParams(arg.dealias)
    })
  }

  def definition(tpe: Type): Definition = {
    val properties = if (playJava) {
      val _mapper = new ObjectMapper();
      val m = runtimeMirror(getClass.getClassLoader)
      val clazz = m.runtimeClass(tpe.typeSymbol.asClass)
      val `type` = _mapper.constructType(clazz)
      val beanDesc: BeanDescription = _mapper.getSerializationConfig.introspect(`type`)
      val beanProperties = beanDesc.findProperties()
      val ignoreProperties = beanDesc.getIgnoredPropertyNames
      val propertySet = JavaConverters.asScalaIteratorConverter(beanProperties.iterator()).asScala.toSeq
      propertySet.filter(bd ⇒ !ignoreProperties.contains(bd.getName)).map { entry ⇒
        val name = entry.getName
        var typeName = entry.getPrimaryType.getRawClass.getName
        if (entry.getField != null) {
          if (entry.getField.getType.hasGenericTypes) {
            val generalType = entry.getField.getType.getContentType.getRawClass.getName
            typeName = s"$typeName[$generalType]"
          }
        }
        if (!entry.isRequired) {
          typeName = s"Option[$typeName]"
        }
        val param = Parameter(name, typeName, None, None)
        mapParam(param, modelQualifier, mappings)
      }
    } else {
      val fields = tpe.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor || m.isFinal ⇒ m
      }.toList.flatMap(_.paramLists).headOption.getOrElse(Nil)

      fields.map { field ⇒
        //TODO: find a better way to get the string representation of typeSignature
        val name = field.name.decodedName.toString
        val typeName = dealiasParams(field.typeSignature).toString
        // passing None for 'fixed' and 'default' here, since we're not dealing with route parameters
        val param = Parameter(name, typeName, None, None)
        mapParam(param, modelQualifier, mappings)
      }
    }

    Definition(
      name = tpe.typeSymbol.fullName,
      properties = properties)
  }

  def definition[T: TypeTag]: Definition = definition(weakTypeOf[T])

  def definition(className: String): Definition = {
    val mirror = runtimeMirror(cl)
    val sym = mirror.staticClass(className)
    val tpe = sym.selfType
    definition(tpe)
  }

  def allDefinitions(typeNames: Seq[String]): List[Definition] = {
    def genSwaggerParameter: PartialFunction[SwaggerParameter, GenSwaggerParameter] =
      { case p: GenSwaggerParameter ⇒ p }

    def allReferredDefs(defName: String, memo: List[Definition]): List[Definition] = {
      memo.find(_.name == defName) match {
        case Some(_) ⇒ memo
        case None ⇒
          val thisDef = definition(defName)
          val refNames: Seq[String] = for {
            p ← thisDef.properties.collect(genSwaggerParameter)
            className ← p.referenceType orElse p.items.collect(genSwaggerParameter).flatMap(_.referenceType)
            if modelQualifier.isModel(className)
          } yield className

          refNames.foldLeft(thisDef :: memo) { (foundDefs, refName) ⇒
            allReferredDefs(refName, foundDefs)
          }
      }
    }

    typeNames.foldLeft(List.empty[Definition]) { (memo, typeName) ⇒
      allReferredDefs(typeName, memo)
    }
  }
}

object DefinitionGenerator {
  def apply(
    playJava:                    Boolean,
    domainNameSpace:             String,
    customParameterTypeMappings: CustomMappings)(implicit cl: ClassLoader): DefinitionGenerator =
    DefinitionGenerator(
      playJava,
      PrefixDomainModelQualifier(domainNameSpace), customParameterTypeMappings)
}
