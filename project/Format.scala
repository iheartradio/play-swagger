import sbt._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object Format {
  lazy val settings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile  := formattingPreferences,
    ScalariformKeys.preferences in Test     := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  }

}
