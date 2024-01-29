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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.RecoverMethods
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames, RequestId}
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.config.{AppContext, ApplicationConfig}
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesError, ProxyCacheData}
import utils.TestData._
import utils.WireMockHelper

class ProxyCacheConnectorSpec
  extends NationalInsuranceRecordUnitSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ScalaFutures
    with RecoverMethods {

  server.start()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.ni-and-sp-proxy-cache.host" -> "127.0.0.1",
      "microservice.services.ni-and-sp-proxy-cache.port" -> server.port(),
      "auditing.enabled" -> false
    )
    .build()

  private val connector: ProxyCacheConnector =
    app.injector.instanceOf[ProxyCacheConnector]
  private val appContext: AppContext =
    app.injector.instanceOf[AppContext]
  private val appConfig: ApplicationConfig =
    app.injector.instanceOf[ApplicationConfig]

  override val headerCarrier: HeaderCarrier =
    HeaderCarrier(
      authorization = Some(Authorization(appContext.internalAuthToken)),
      requestId     = Some(RequestId("requestId"))
    )

  private val nino: Nino =
    generateNino()

  private val url: String =
    s"/ni-and-sp-proxy-cache/${nino.nino}"

  private val requests: Seq[(ResponseDefinitionBuilder, String)] = Seq(
    serverError() -> "internalServerError",
    badRequest() -> "badRequest",
    aResponse().withStatus(502) -> "gatewayTimeout",
    serviceUnavailable() -> "serviceUnavailable"
  )

  "getProxyCacheData success" should {

    "return Right(ProxyCacheData) when json can be parsed" in {
      server.stubFor(get(urlEqualTo(url))
        .willReturn(ok(Json.stringify(Json.toJson(proxyCacheData)))))

      val result: Either[DesError, ProxyCacheData] = await(connector.get(nino)(headerCarrier))

      result map { proxyCacheData =>
        proxyCacheData.liabilities shouldBe desLiabilities
        proxyCacheData.summary shouldBe desSummary
        proxyCacheData.niRecord shouldBe desNIRecord
      }
    }
  }

  "getProxyCacheData unsuccessful" should {
    requests.foreach {
      case (errorResponse, description) =>

      s"return Left(DesError) for $description" in {
        server.stubFor(get(urlEqualTo(url))
          .willReturn(errorResponse))

        val response: Either[DesError, ProxyCacheData] = await(connector.get(nino)(headerCarrier))
        response.left.map(_ shouldBe a[DesError.HttpError])
      }
    }

    "return default Left(DesError)" in {
      server.stubFor(get(urlEqualTo(url))
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

      val response: Either[DesError, ProxyCacheData] = await(connector.get(nino)(headerCarrier))

      response.left.map(_ shouldBe a[DesError.OtherError])
    }
  }

  "headers" should {
    "be present" in {

      server.stubFor(get(urlEqualTo(url))
        .willReturn(ok(Json.stringify(Json.toJson(proxyCacheData)))))

      await(connector.get(nino)(headerCarrier))

      server.verify(getRequestedFor(urlEqualTo(url))
        .withHeader(HeaderNames.authorisation, equalTo(appContext.internalAuthToken))
        .withHeader("Originator-Id", equalTo("DA_PF"))
        .withHeader("Environment", equalTo(appConfig.desEnvironment))
        .withHeader("CorrelationId", matching("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
        .withHeader(HeaderNames.xRequestId, equalTo("requestId"))
      )
    }
  }
}
