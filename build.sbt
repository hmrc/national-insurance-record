
import play.sbt.routes.RoutesKeys._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import scoverage.ScoverageKeys

val appName = "national-insurance-record"

lazy val scoverageSettings: Seq[Def.Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;" +
      ".*(AuthService|BuildInfo|Routes).*;" +
      "connectors.*",
    ScoverageKeys.coverageMinimumStmtTotal := 83.54,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
)

val suppressedImports = Seq("-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlFeatureImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlHelperImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Html",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.JavaScript",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Txt",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Xml",
  "-P:silencer:lineContentFilters=import models._",
  "-P:silencer:lineContentFilters=import controllers._",
  "-P:silencer:lineContentFilters=import play.api.i18n._",
  "-P:silencer:lineContentFilters=import views.txt._",
  "-P:silencer:lineContentFilters=import play.api.templates.PlayMagic._",
  "-P:silencer:lineContentFilters=import play.api.mvc._",
  "-P:silencer:lineContentFilters=import play.api.data._",
  "-P:silencer:lineContentFilters=import play.api.libs._")

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    scalaSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 0,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-P:silencer:pathFilters=routes"
    ),
    scalacOptions ++= suppressedImports,
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
