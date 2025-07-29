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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.*
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.{AuthConnector, UnsupportedCredentialRole}
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.nationalinsurancerecord.util.UnitSpec
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.MdtpAuthActionSpec.retrievalsTestingSyntax
import org.scalatest.matchers.must.Matchers.mustBe

import scala.concurrent.Future

class MdtpAuthActionSpec extends UnitSpec {

  private val ninoGenerator: Generator = new Generator()
  private val testNino: String = ninoGenerator.nextNino.nino
  val notTestNino = testNino.take(testNino.length - 1) + "X"
  private val goodUriWithNino: String = s"/ni/$testNino/"

  class AuthActionTestHarness(mdtpAuthActionImpl: MdtpAuthActionImpl) extends BackendController(Helpers.stubControllerComponents()) {
    def onPageLoad(): Action[AnyContent] = mdtpAuthActionImpl { request =>
      Ok
    }
  }

  private def newMockConnectorWithAuthResult[T](authoriseResult: Future[T]): AuthConnector = {
    val connector = mock[AuthConnector]

    when(connector.authorise[T](any(), any())(any(), any()))
      .thenReturn(authoriseResult)

    connector
  }

  private def testMdtpAuthActionWith[T](authResult: Future[T],
                                        uri: String = goodUriWithNino): (Future[Result], AuthConnector) = {
    val mockAuthConnector = newMockConnectorWithAuthResult(authResult)

    val injector = new GuiceApplicationBuilder()
      .overrides(inject.bind[AuthConnector].toInstance(mockAuthConnector))
      .injector()

    val authAction = injector.instanceOf[MdtpAuthActionImpl]

    val testHarness = new AuthActionTestHarness(authAction)

    (testHarness.onPageLoad()(FakeRequest(method = "", path = uri)),
      mockAuthConnector)
  }

  "MdtpAuthAction" should {
    "return ok when valid" in {
      val (result, _) = testMdtpAuthActionWith(Future.successful(Some(testNino) ~ Some(TrustedHelper("", "", "", Some(notTestNino)))))
      status(result) mustBe OK
    }

    "return unauthorised when nino does not match nino in uri" in {
      val (result, _) = testMdtpAuthActionWith(Future.successful(None ~ Some(TrustedHelper("", "", "", Some(notTestNino)))))
      status(result) mustBe UNAUTHORIZED
    }

    "return bad request when there are no matches in uri" in {
      val (result, _) = testMdtpAuthActionWith(Future.successful(None ~ Some(TrustedHelper("", "", "", Some(notTestNino)))), "badURI")
      status(result) mustBe BAD_REQUEST
    }

    "return unauthorized when retrievals returns both none" in {
      val (result, _) = testMdtpAuthActionWith(Future.successful(None ~ None))
      status(result) mustBe UNAUTHORIZED
    }

    "return INTERNAL_SERVER_ERROR" when {
      "auth returns an unexpected exception" in {
        val (result, _) = testMdtpAuthActionWith(Future.failed(new Exception("")))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "auth returns an unexpected authorisation error" in {
      val (result, _) = testMdtpAuthActionWith(Future.failed(UnsupportedCredentialRole()))
      status(result) mustBe UNAUTHORIZED
    }
  }

}

object MdtpAuthActionSpec {
  implicit class retrievalsTestingSyntax[A](val a: A) extends AnyVal {
    def ~[B](b: B): A ~ B = new~(a, b)
  }
}
