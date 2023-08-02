package uk.gov.hmrc.nationalinsurancerecord.config

import akka.Done
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, getRequestedFor, post, postRequestedFor, urlMatching}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

class InternalAuthTokenInitializerISpec extends IntegrationBaseSpec {

  "AuthTokenInitializer" should {
    "return Done with no requests sent to internal-auth" when {
      "isTestEndpoints is configured to false" in {

        val authToken = "authToken"
        val appName = "appName"

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "internal-auth.token" -> authToken,
            "internal-auth.isTestOnlyEndpoint" -> false
          )
          .build()

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(0, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }

      "isTestEndpoints is configured to true and token is already valid" in {
        val authToken = "authToken"
        val appName = "appName"

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(OK))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "internal-auth.token" -> authToken
          )
          .build()

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(1, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(0, postRequestedFor(urlMatching("/test-only/token")))
      }

      "isTestEndpoints is configured to true and token needs intitializing" in {
        val authToken = "authToken"
        val appName = "appName"

        server.stubFor(
          get(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        server.stubFor(
          post(urlMatching("/test-only/token"))
            .willReturn(aResponse().withStatus(CREATED))
        )

        val app = GuiceApplicationBuilder()
          .overrides(
            bind[AsyncCacheApi].toInstance(mockCacheApi)
          )
          .configure(
            "microservice.services.internal-auth.port" -> server.port(),
            "appName" -> appName,
            "internal-auth.token" -> authToken
          )
          .build()

        app.injector.instanceOf[InternalAuthTokenInitializer].initializeToken.futureValue shouldBe Done

        server.verify(1, getRequestedFor(urlMatching("/test-only/token")))
        server.verify(1, postRequestedFor(urlMatching("/test-only/token")))
      }
    }
  }
}
