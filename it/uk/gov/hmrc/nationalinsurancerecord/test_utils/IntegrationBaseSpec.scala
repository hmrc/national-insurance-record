package uk.gov.hmrc.nationalinsurancerecord.test_utils

import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.domain.{Generator, Nino}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.Random

trait IntegrationBaseSpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockHelper {

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def generateNino: Nino = new Generator(new Random).nextNino

  def generateUUId: UUID = UUID.randomUUID()

  def mock[T](implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]])

  def mock[T](answer: Answer[Object])(implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]], answer)
}