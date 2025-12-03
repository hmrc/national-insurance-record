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

package uk.gov.hmrc.nationalinsurancerecord.config

import org.apache.pekko.Done
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, getRequestedFor, ok, post, postRequestedFor, urlMatching}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

class InternalAuthTokenInitializerISpec extends IntegrationBaseSpec {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AsyncCacheApi].toInstance(mockCacheApi)
    )
    .configure(
      "microservice.services.internal-auth.port" -> server.port(),
      "appName" -> "appName",
      "internal-auth.token" -> "authToken",
      "internal-auth.isTestOnlyEndpoint" -> false
    ).build()

  "AuthTokenInitializer" should {
    "return Done with no requests sent to internal-auth" when {
      "isTestEndpoints is configured to false" in {

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(0, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }

      "isTestEndpoints is configured to true and token is already valid" in {

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        def app(): Application = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> "appName",
            "internal-auth.token" -> "authToken",
            "internal-auth.isTestOnlyEndpoint" -> true
          ).build()

        app().injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(1, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }

      "isTestEndpoints is configured to true and token needs intitializing" in {

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        def app(): Application = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> "appName",
            "internal-auth.token" -> "authToken",
            "internal-auth.isTestOnlyEndpoint" -> true
          ).build()

        app().injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(1, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(1, postRequestedFor(urlMatching("/test-only/token")))
      }
    }
  }
}
