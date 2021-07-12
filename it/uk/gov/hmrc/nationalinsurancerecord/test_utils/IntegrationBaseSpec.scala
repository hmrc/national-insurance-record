package uk.gov.hmrc.nationalinsurancerecord.test_utils

import java.util.UUID
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.domain.{Generator, Nino}

import scala.concurrent.ExecutionContext
import scala.util.Random

trait IntegrationBaseSpec extends WordSpec
  with MustMatchers
  with MockitoSugar
  with GuiceOneAppPerSuite
  with WireMockHelper {

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def generateNino: Nino = new Generator(new Random).nextNino

  def generateUUId: UUID = UUID.randomUUID()
}