package com.iheart.playSwagger

import scala.collection.immutable

import enumeratum.EnumEntry.Snakecase
import enumeratum._

sealed trait SampleEnumeratumEnum extends EnumEntry with Snakecase

object SampleEnumeratumEnum extends Enum[SampleEnumeratumEnum] {
  val values: immutable.IndexedSeq[SampleEnumeratumEnum] = findValues

  case object InfoOne extends SampleEnumeratumEnum
  case object InfoTwo extends SampleEnumeratumEnum
}
