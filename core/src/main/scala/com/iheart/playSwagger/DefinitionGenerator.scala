package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.{ CustomMappings, SwaggerParameter, GenSwaggerParameter, Definition }
import com.iheart.playSwagger.SwaggerParameterMapper.mapParam
import play.routes.compiler.Parameter

import scala.reflect.runtime.universe._

final case class DefinitionGenerator(
  modelQualifier:  DomainModelQualifier      = PrefixDomainModelQualifier(),
  mappings:        CustomMappings            = Nil,
  nameTransformer: DefinitionNameTransformer = new NoTransformer)(implicit cl: ClassLoader) {

  def dealiasParams(t: Type): Type = {
    appliedType(t.dealias.typeConstructor, t.typeArgs.map { arg ⇒
      dealiasParams(arg.dealias)
    })
  }

  def definition(tpe: Type): Definition = {
    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.toList.flatMap(_.paramLists).headOption.getOrElse(Nil)

    val properties = fields.map { field ⇒
      //TODO: find a better way to get the string representation of typeSignature
      val name = field.name.decodedName.toString
      val typeName = dealiasParams(field.typeSignature).toString
      // passing None for 'fixed' and 'default' here, since we're not dealing with route parameters
      val param = Parameter(name, typeName, None, None)
      mapParam(param, nameTransformer, modelQualifier, mappings)
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
    domainNameSpace:             String,
    customParameterTypeMappings: CustomMappings,
    nameTransformer:             DefinitionNameTransformer)(implicit cl: ClassLoader): DefinitionGenerator =
    DefinitionGenerator(
      PrefixDomainModelQualifier(domainNameSpace), customParameterTypeMappings, nameTransformer)
}
