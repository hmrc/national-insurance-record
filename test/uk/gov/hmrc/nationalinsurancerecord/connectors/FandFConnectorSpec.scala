/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class FandFConnectorSpec extends NationalInsuranceRecordUnitSpec
  with WireMockSupport
  with GuiceOneAppPerSuite
  with HttpClientV2Support
  with ScalaFutures
  with Injecting {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val trustedHelperNino = new Generator().nextNino

  val trustedHelper: TrustedHelper =
    TrustedHelper("principal Name", "attorneyName", "returnLink", Some(trustedHelperNino.nino))

  val fandfTrustedHelperResponse: String =
    s"""
       |{
       |   "principalName": "principal Name",
       |   "attorneyName": "attorneyName",
       |   "returnLinkUrl": "returnLink",
       |   "principalNino": "$trustedHelperNino"
       |}
       |""".stripMargin

  lazy val injector: Injector = 
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.fandf.port" -> wireMockPort,
        "microservice.services.fandf.host" -> wireMockHost,
      ).injector()
  
  lazy val connector: FandFConnector = new FandFConnector(
    injector.instanceOf[HttpClientV2],
    injector.instanceOf[ApplicationConfig]
  )

  "Calling FandFConnector.getTrustedHelper" must {
    "return as Some(trustedHelper) when trustedHelper json returned" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/delegation/get")).willReturn(ok(fandfTrustedHelperResponse))
      )

      val result: Option[TrustedHelper] = Await.result(connector.getTrustedHelper, Duration.Inf)

      result shouldBe Some(trustedHelper)
    }

    "return as None when not found returned" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/delegation/get")).willReturn(notFound())
      )

      val result: Option[TrustedHelper] = Await.result(connector.getTrustedHelper, Duration.Inf)

      result shouldBe None
    }

    "return None when error status returned" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/delegation/get")).willReturn(serverError())
      )
      val result = Await.result(connector.getTrustedHelper, Duration.Inf)

      result shouldBe None
    }

    "return None when unexpected status returned" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/delegation/get")).willReturn(noContent())
      )
      val result = Await.result(connector.getTrustedHelper, Duration.Inf)

      result shouldBe None
    }

  }
}