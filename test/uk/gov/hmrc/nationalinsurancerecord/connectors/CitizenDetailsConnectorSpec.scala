/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.services.MetricsService

import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends NationalInsuranceRecordUnitSpec with BeforeAndAfter with ScalaFutures with GuiceOneAppPerSuite {
  // scalastyle:off magic.number

  val nino = generateNino()
  lazy val fakeRequest = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockMetrics: MetricsService = mock[MetricsService]
  val mockTimerContext = mock[Timer.Context]
  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClientV2].toInstance(mockHttp),
      bind[MetricsService].toInstance(mockMetrics)
    )
    .build()

  val citizenDetailsConnector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  def beforeEach(): Unit = {
    Mockito.reset(mockHttp)
    Mockito.reset(mockMetrics)
  }

  "CitizenDetailsConnector" must {

    "return OK status when successful" in {
      when(mockMetrics.startCitizenDetailsTimer()).thenReturn(mockTimerContext)
      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())).thenReturn(
        Future.successful(Right(HttpResponse(200, "")))
      )
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF) shouldBe Right(200)
    }

    "return 423 status when the Upstream is 423" in {
      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())).thenReturn(
        Future.successful(Left(UpstreamErrorResponse(":(", 423, 423, Map()))
      ))
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF) shouldBe Right(423)
    }

    "return NotFoundException for invalid nino" in {
      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())).thenReturn(
        Future.successful(Left(UpstreamErrorResponse("NOT_FOUND", 404))
      ))
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF) shouldBe Left(UpstreamErrorResponse("NOT_FOUND", 404))
    }

    "return 500 response code when there is Upstream is 4XX" in {
      when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())).thenReturn(
        Future.failed(new InternalServerException("InternalServerError"))
      )
      val resultF = citizenDetailsConnector.retrieveMCIStatus(nino)(hc)
      await(resultF.failed) shouldBe a[InternalServerException]
    }

    "log correct CitizenDetailsConnector metrics" in {
      verify(mockMetrics, atLeastOnce()).startCitizenDetailsTimer()
      verify(mockTimerContext, atLeastOnce()).stop()
    }
  }
}
