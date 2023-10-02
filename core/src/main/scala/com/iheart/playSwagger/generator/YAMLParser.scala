package com.iheart.playSwagger.generator

import com.fasterxml.jackson.databind.ObjectMapper
import org.yaml.snakeyaml.Yaml
import play.api.libs.json.{Json, Reads}

object YAMLParser {

  def parseYaml[T](document: String)(implicit fjs: Reads[T]): T = {
    val yaml = new Yaml()
    val map = yaml.load[T](document)
    val mapper = new ObjectMapper()
    val jsonString = mapper.writeValueAsString(map)
    Json.parse(jsonString).as[T]
  }

}
