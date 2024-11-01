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

package uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nationalinsurancerecord.domain.ProxyCacheToggle
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

import scala.concurrent.Future

trait NationalInsuranceRecordControllerISpec extends IntegrationBaseSpec with Results {
  def classPrefix: String
  def controllerUrl(nino: Nino): String

  server.start()

  protected val mockFeatureFlagService: FeatureFlagService =
    mock[FeatureFlagService]

  protected val wiremockConfig: Map[String, Any] = Map(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.pertax.port" -> server.port(),
    "microservice.services.des-hod.port" -> server.port(),
    "microservice.services.ni-and-sp-proxy-cache.port" -> server.port(),
    "microservice.services.citizen-details.port" -> server.port(),
    "auditing.enabled" -> false,
    "internal-auth.isTestOnlyEndpoint" -> false
  )

  private val nino: Nino = generateNino

  override def beforeEach(): Unit = {
    super.beforeEach()

    val authResponse =
      s"""
         |{
         | "nino": "$nino",
         | "authProviderId": { "ggCredId": "xyz" }
         |}
         |""".stripMargin

    val pertaxAuthResponse =
      s"""
         |{
         | "code": "ACCESS_GRANTED",
         | "message": ""
         |}
         |""".stripMargin

    stubPostServer(ok(authResponse), "/auth/authorise")
    stubPostServer(ok(pertaxAuthResponse), "/pertax/authorise")
    stubGetServer(ok(""), s"/citizen-details/${nino.nino}/designatory-details/")
  }

  private val requests = List(
    badRequest()     -> "400" -> BAD_REQUEST,
    unauthorized()   -> "401" -> BAD_GATEWAY,
    notFound()       -> "404" -> NOT_FOUND,
    serverError()    -> "500" -> BAD_GATEWAY,
    badGateway()     -> "502" -> BAD_GATEWAY,
    gatewayTimeout() -> "504" -> GATEWAY_TIMEOUT
  )

  private val desNIUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/ni"
  private val desSummaryUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/summary"
  private val desLiabilitiesUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/liabilities"

  private val proxyCacheUrl: String = s"/ni-and-sp-proxy-cache/${nino.nino}"

  s"${classPrefix}NationalInsuranceRecordController" must {

    requests.foreach {
      case ((errorResponse, errorCode), statusCode) =>
        s"return status code $statusCode for $errorCode when ProxyCacheToggle is disabled" in {
          when(mockFeatureFlagService.get(ArgumentMatchers.any[FeatureFlagName]()))
            .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false)))

          stubGetServer(errorResponse, desNIUrl)
          stubGetServer(errorResponse, desSummaryUrl)
          stubGetServer(errorResponse, desLiabilitiesUrl)

          val request = FakeRequest(GET, controllerUrl(nino))
            .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
            .withHeaders("Authorization" -> "Bearer 123")

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }

  s"${classPrefix}NationalInsuranceRecordController" must {

    requests.foreach {
      case ((errorResponse, errorCode), statusCode) =>
        s"return status code $statusCode for $errorCode when ProxyCacheToggle is enabled" in {
          when(mockFeatureFlagService.get(ArgumentMatchers.any[FeatureFlagName]()))
            .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = true)))

          stubGetServer(errorResponse, proxyCacheUrl)

          val request = FakeRequest(GET, controllerUrl(nino))
            .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
            .withHeaders("Authorization" -> "Bearer 123")

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }
}
