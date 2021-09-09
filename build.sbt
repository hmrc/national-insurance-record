
import play.sbt.routes.RoutesKeys._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
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
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 0,
    scalaVersion := "2.12.13",
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    PlayKeys.playDefaultPort := 9312,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesImport += "uk.gov.hmrc.nationalinsurancerecord.config.Binders._",
    routesGenerator := InjectedRoutesGenerator
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    parallelExecution in IntegrationTest := false
  )
