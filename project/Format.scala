import sbt._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtScalariform.autoImport.scalariformAutoformat

object Format {
  lazy val settings = Seq(
    scalariformAutoformat := false,
    Compile / ScalariformKeys.preferences := formattingPreferences,
    Test / ScalariformKeys.preferences := formattingPreferences)

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  }
}
