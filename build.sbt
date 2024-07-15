
import play.sbt.routes.RoutesKeys.*
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}
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

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.14"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    scalaSettings,
    defaultSettings(),
    scalacOptions ++= Seq(
      "-Werror",
      "-Wconf:src=routes/.*:is,src=twirl/.*:is"
    ),
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    PlayKeys.playDefaultPort := 9312,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesImport ++= Seq(
      "uk.gov.hmrc.nationalinsurancerecord.config.Binders._",
      "uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"
    ),
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(inConfig(Test)(testSettings): _*)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils" / "src",
  Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
)

lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= AppDependencies.test,
    DefaultBuildSettings.itSettings()
  )
