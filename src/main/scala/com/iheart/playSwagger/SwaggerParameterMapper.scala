package com.iheart.playSwagger

import com.iheart.playSwagger.Domain.SwaggerParameter
import org.joda.time.DateTime

object SwaggerParameterMapper {
  def mapParam(name: String, scalaTypeName: String, domainNameSpace: Option[String] = None): SwaggerParameter = {

    def higherOrderType(higherOrder: String, typeName: String): Option[String] =  s"$higherOrder\\[(\\S+)\\]".r.findFirstMatchIn(typeName).map(_.group(1))

    def collectionItemType(typeName: String): Option[String] =
      List("Seq", "List", "Set", "Vector").map(higherOrderType(_, typeName)).reduce(_ orElse _)


    def prop(tp: String, format: Option[String] = None, required: Boolean = true) =
      SwaggerParameter(name, `type` = Some(tp), format = format, required = required)


    val typeName = scalaTypeName.replace("scala.", "").replace("java.lang.", "")

    if(domainNameSpace.fold(false)(typeName.startsWith(_)))
      SwaggerParameter(name, referenceType = Some(typeName))
    else {
      val optionalType = higherOrderType("Option", typeName)
      val itemType = collectionItemType(typeName)
      if(itemType.isDefined)
        SwaggerParameter(name, items = itemType)
      else if (optionalType.isDefined)
        mapParam(name, optionalType.get).copy(required = false)
      else
        typeName match {
          case "Int"      ⇒ prop("integer", Some("int32"))
          case "Long"     ⇒ prop("integer", Some("int64"))
          case "Double"   ⇒ prop("number", Some("double"))
          case "Float"    ⇒ prop("number", Some("float"))
          case "org.jodaTime.DateTime" ⇒ prop("integer", Some("epoch"))
          case unknown    ⇒ prop(unknown.toLowerCase())
        }
    }

  }
}
