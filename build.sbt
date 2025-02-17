
import play.sbt.routes.RoutesKeys.*
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}
import scoverage.ScoverageKeys

val appName = "national-insurance-record"

lazy val scoverageSettings: Seq[Def.Setting[?]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;views.*;config.*;models.*;.*domain.*;.*AppContext.*;" +
      ".*(AuthService|BuildInfo|Routes).*;" +
      "connectors.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
)

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.6.2"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins *)
  .settings(
    scoverageSettings,
    scalaSettings,
    defaultSettings(),
    scalacOptions ++= Seq(
      "-feature",
      "-Xfatal-warnings",
      "-Wconf:src=target/.*:s",
      "-Wconf:src=routes/.*:s",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:msg=.*-Wunused.*:s"
    ),
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    PlayKeys.playDefaultPort := 9312,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesImport ++= Seq(
      "uk.gov.hmrc.nationalinsurancerecord.config.Binders._"
    ),
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(inConfig(Test)(testSettings) *)

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
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
