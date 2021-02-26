
import play.sbt.routes.RoutesKeys._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "national-insurance-record"

lazy val scoverageSettings: Seq[Def.Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*;" +
      "connectors.*",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    scalaVersion := "2.12.12",
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    PlayKeys.playDefaultPort := 9312,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesImport += "uk.gov.hmrc.nationalinsurancerecord.config.Binders._",
    routesGenerator := InjectedRoutesGenerator,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    )
  )