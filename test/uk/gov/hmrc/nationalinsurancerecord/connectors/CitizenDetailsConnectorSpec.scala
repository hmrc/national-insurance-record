/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nationalinsurancerecord.connectors

import com.codahale.metrics.Timer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.services.MetricsService

import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends NationalInsuranceRecordUnitSpec with BeforeAndAfter with ScalaFutures with GuiceOneAppPerSuite {
  // scalastyle:off magic.number

  val nino = generateNino()
  lazy val fakeRequest = FakeRequest()
  implicit val hc = HeaderCarrier()
  val mockMetrics: MetricsService = mock[MetricsService]
  val mockTimerContext = mock[Timer.Context]
  val mockHttp: HttpClient = mock[HttpClient]
  val mockDesConnector: DesConnector = mock[DesConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mockHttp),
      bind[MetricsService].toInstance(mockMetrics),
      bind[DesConnector].toInstance(mockDesConnector)
    )
    .build()

  val citizenDetailsConnector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  def beforeEach(): Unit = {
    Mockito.reset(mockHttp)
    Mockito.reset(mockMetrics)
  }

  "CitizenDetailsConnector" must{

    "return OK status when successful" in {
      when(mockMetrics.startCitizenDetailsTimer()).thenReturn(mockTimerContext)
      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any())) thenReturn Future.successful(HttpResponse(200, ""))
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF) shouldBe 200
    }

    "return 423 status when the Upstream is 423" in {
      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any())) thenReturn
        Future.failed(UpstreamErrorResponse(":(", 423, 423, Map()))
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF) shouldBe 423
    }

    "return NotFoundException for invalid nino" in {
      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any())) thenReturn
        Future.failed(new NotFoundException("Not Found"))
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF.failed) shouldBe a[NotFoundException]
    }

    "return 500 response code when there is Upstream is 4XX" in {
      when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any())) thenReturn
        Future.failed(new InternalServerException("InternalServerError"))
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF.failed) shouldBe a[InternalServerException]
    }

    "log correct CitizenDetailsConnector metrics" in {
      verify(mockMetrics, atLeastOnce()).startCitizenDetailsTimer()
      verify(mockTimerContext, atLeastOnce()).stop()
    }
  }
}
