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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.CopeExclusionAction
import uk.gov.hmrc.nationalinsurancerecord.domain.ProxyCacheToggle
import uk.gov.hmrc.nationalinsurancerecord.test_utils.{FakeAction, IntegrationBaseSpec}

import scala.concurrent.Future

class NationalInsuranceRecordControllerISpec extends IntegrationBaseSpec with Results {

  private val mockCopeExclusionAction: CopeExclusionAction =
    mock[CopeExclusionAction]

  private val mockFeatureFlagService: FeatureFlagService =
    mock[FeatureFlagService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[CopeExclusionAction].to(mockCopeExclusionAction),
      bind[AsyncCacheApi].toInstance(mockCacheApi),
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .configure(
      "microservice.services.auth.port" -> server.port(),
      "microservice.services.des-hod.host" -> "127.0.0.1",
      "microservice.services.des-hod.port" -> server.port(),
      "microservice.services.ni-and-sp-proxy-cache.host" -> "127.0.0.1",
      "microservice.services.ni-and-sp-proxy-cache.port" -> server.port(),
      "auditing.enabled" -> false
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    val authResponse =
      s"""
        |{
        | "nino": "$nino",
        | "authProviderId": { "ggCredId": "xyz" }
        |}
        |""".stripMargin

    stubPostServer(ok(authResponse), "/auth/authorise")
    when(mockCopeExclusionAction.filterCopeExclusions(any()))
      .thenReturn(new FakeAction[AnyContent]())
  }

  private val nino: Nino = generateNino

  private val requests = List(
    notFound() -> "404" -> NOT_FOUND,
    badRequest() -> "400" -> BAD_REQUEST,
    gatewayTimeout() -> "504" -> GATEWAY_TIMEOUT,
    badGateway() -> "502" -> BAD_GATEWAY,
    serverError() -> "500" -> BAD_GATEWAY,
    unauthorized() -> "401" -> BAD_GATEWAY
  )

  private val controllerUrl: String = s"/ni/$nino"

  private val desUrl: String = s"/individuals/${nino.withoutSuffix}/pensions/ni"

  private val proxyCacheUrl: String = s"/ni-and-sp-proxy-cache/${nino.nino}"

  "NationalInsuranceRecordController" must {

    requests.foreach {
      case ((errorResponse, errorCode), statusCode) =>
        s"return status code $statusCode for $errorCode" in {
          when(mockFeatureFlagService.get(ArgumentMatchers.any[FeatureFlagName]()))
            .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false)))

          stubGetServer(errorResponse, desUrl)

          val request = FakeRequest(GET, controllerUrl)
            .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
            .withHeaders("Authorization" -> "Bearer 123")

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }

  "NationalInsuranceRecordController when ProxyCacheToggle is enabled" must {

    requests.foreach {
      case ((errorResponse, errorCode), statusCode) =>
        s"return status code $statusCode for $errorCode" in {
          when(mockFeatureFlagService.get(ArgumentMatchers.any[FeatureFlagName]()))
            .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = true)))

          stubGetServer(errorResponse, proxyCacheUrl)

          val request = FakeRequest(GET, controllerUrl)
            .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
            .withHeaders("Authorization" -> "Bearer 123")

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }
}
