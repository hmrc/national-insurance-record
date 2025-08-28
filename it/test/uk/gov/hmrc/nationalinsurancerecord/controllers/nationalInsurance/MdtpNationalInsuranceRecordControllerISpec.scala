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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status as statusResult, *}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.MdtpCopeExclusionAction
import uk.gov.hmrc.nationalinsurancerecord.test_utils.{FakeAction, IntegrationBaseSpec}

class MdtpNationalInsuranceRecordControllerISpec extends IntegrationBaseSpec with Results {

  server.start()

  override def beforeEach(): Unit = {
    super.beforeEach()

    val authResponse =
      s"""
         |{
         |  "nino": "$nino",
         |  "authProviderId": {
         |    "ggCredId": "xyz"
         |  }
         |}"""
        .stripMargin

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

  private val mockCopeExclusionAction: MdtpCopeExclusionAction = mock[MdtpCopeExclusionAction]

  protected val wiremockConfig: Map[String, Any] = Map(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.pertax.port" -> server.port(),
    "microservice.services.ni-and-sp-proxy-cache.port" -> server.port(),
    "microservice.services.citizen-details.port" -> server.port(),
    "internal-auth.isTestOnlyEndpoint" -> false
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MdtpCopeExclusionAction].to(mockCopeExclusionAction),
      bind[AsyncCacheApi].toInstance(mockCacheApi)
    )
    .configure(
      wiremockConfig
    ).build()

  when(mockCopeExclusionAction.filterCopeExclusions(any()))
    .thenReturn(new FakeAction[AnyContent]())

  private val nino: Nino = generateNino
  private val proxyCacheUrl: String = s"/ni-and-sp-proxy-cache/${nino.nino}"

  private val requests = List(
    badRequest()     -> "400" -> BAD_REQUEST,
    unauthorized()   -> "401" -> BAD_GATEWAY,
    notFound()       -> "404" -> NOT_FOUND,
    serverError()    -> "500" -> BAD_GATEWAY,
    badGateway()     -> "502" -> BAD_GATEWAY,
    gatewayTimeout() -> "504" -> GATEWAY_TIMEOUT
  )

  "MtdpNationalInsuranceRecordController" must {
    
    requests.foreach {
      case ((errorResponse, errorCode), statusCode) =>
        s"return status code $statusCode for $errorCode" in {
          stubGetServer(errorResponse, proxyCacheUrl)

          val request = FakeRequest(GET, s"/ni/mdtp/$nino")
            .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
            .withHeaders("Authorization" -> "Bearer 123")

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }
}
