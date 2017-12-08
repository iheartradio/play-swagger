package com.iheart.playSwagger

trait DefinitionNameTransformer {
  def transform(str: String): String
}

final class NoTransformer extends DefinitionNameTransformer {
  override def transform(str: String) = str
}