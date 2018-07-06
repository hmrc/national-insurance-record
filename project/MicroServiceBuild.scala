import play.sbt.routes.RoutesKeys
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "national-insurance-record"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  override lazy val playSettings = Seq(RoutesKeys.routesImport += "uk.gov.hmrc.domain.Nino")
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "6.18.0"
  private val domainVersion = "5.1.0"
  private val pegdownVersion = "1.6.0"
  private val playHmrcApiVersion = "2.1.0"
  private val playHalVersion = "1.2.0"
  private val reactivemongoVersion = "5.2.0"

  private val hmrcTestVersion = "2.4.0"
  private val scalaTestVersion = "2.2.6"
  private val scalaTestPlusVersion = "1.5.1"
  private val mockitoAllVersion = "1.9.5"
  private val reactivemongoTestVersion = "2.0.0"

  val compile = Seq(

    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api" % playHmrcApiVersion,
    "uk.gov.hmrc" %% "play-hal" % playHalVersion,
    "uk.gov.hmrc" %% "play-reactivemongo" % reactivemongoVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % reactivemongoVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.mockito" % "mockito-all" % mockitoAllVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
