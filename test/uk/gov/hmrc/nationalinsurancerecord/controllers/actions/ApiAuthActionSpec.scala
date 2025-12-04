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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers.{any, eq => MockitoEq}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.nationalinsurancerecord.util.UnitSpec
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.language.postfixOps

class ApiAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite {

  val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()

  implicit val timeout: Timeout = 5 seconds

  private def newMockConnectorWithAuthResult[T](authoriseResult: Future[T]): AuthConnector = {
    val connector = mock[AuthConnector]

    when(connector.authorise[T](any(), any())(any(), any())).thenReturn(authoriseResult)

    connector
  }

  class AuthActionTestHarness(apiAuthAction: ApiAuthAction) extends BackendController(controllerComponents) {
    def onPageLoad(): Action[AnyContent] = apiAuthAction { _ =>
      Ok
    }
  }

  private def testApiAuthActionWith[T](authResult: Future[T]): (Future[Result], AuthConnector) = {

    val mockAuthConnector = newMockConnectorWithAuthResult(authResult)

    val injector = GuiceApplicationBuilder()
      .overrides(bind[AuthConnector].toInstance(mockAuthConnector)).injector()

    val authAction = injector.instanceOf[ApiAuthAction]
    val testHarness = new AuthActionTestHarness(authAction)

    (testHarness.onPageLoad()(FakeRequest()), mockAuthConnector)
  }


  "Auth Action" when {
    "the user is not logged in" must {
      "return UNAUTHORIZED" in {
        val (result, _) = testApiAuthActionWith(Future.failed(new MissingBearerToken))

        status(result) shouldBe UNAUTHORIZED
      }
    }

    "the user is logged in" must {
      "return the request" when {
        "the user is authorised and Nino matches the Nino in the uri" in {
          val (result, mockAuthConnector) = testApiAuthActionWith(Some(""))

          status(result) shouldBe OK
        }

        "the request comes from a privileged application" in {
          val (result, mockAuthConnector) =
            testApiAuthActionWith(Future.successful(Some("clientId")))

          status(result) shouldBe OK

          verify(mockAuthConnector)
            .authorise[Unit](MockitoEq(AuthProviders(PrivilegedApplication)), any())(any(), any())
        }
      }

      "return UNAUTHORIZED" when {
        "not a Privileged application" in {
          val (result, _) =
            testApiAuthActionWith(Future.failed(new UnsupportedAuthProvider))
          status(result) shouldBe UNAUTHORIZED
        }

      }

    }
    "return INTERNAL_SERVER_ERROR" when {
      "auth returns an unexpected exception" in {
        val (result, _) = testApiAuthActionWith(Future.failed(new Exception("")))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}


object ApiAuthActionSpec {

  implicit class retrievalsTestingSyntax[A](val a: A) extends AnyVal {
    def ~[B](b: B): A ~ B = new~(a, b)
  }

}