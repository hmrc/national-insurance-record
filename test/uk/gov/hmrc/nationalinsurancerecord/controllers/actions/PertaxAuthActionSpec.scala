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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import cats.data.EitherT
import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_GATEWAY, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Result}
import play.mvc.Controller
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nationalinsurancerecord.connectors.PertaxConnector
import uk.gov.hmrc.nationalinsurancerecord.util.UnitSpec
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Future

class PertaxAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite {

  implicit val timeout: Timeout = 5 seconds

  private val CLIENT_CLOSED_REQUEST = 499

  "Pertax Auth Action " should {
    "return 200" when {
      "the user is granted access" in {
        val result = testPertaxAuthActionWith(EitherT.rightT(Future.successful(PertaxAuthResponse("ACCESS_GRANTED", ""))))

        status(result) shouldBe OK
      }
    }

    "return UNAUTHORIZED" when {
      "any code other than ACCESS_GRANTED is returned" in {
        val result = testPertaxAuthActionWith(EitherT.rightT(Future.successful(PertaxAuthResponse("UNAUTHORIZED", ""))))

        status(result) shouldBe UNAUTHORIZED
      }

      "an 'UNAUTHORIZED' error response is received" in {
        val result = testPertaxAuthActionWith(EitherT.leftT(Future.successful(UpstreamErrorResponse("", UNAUTHORIZED))))

        status(result) shouldBe UNAUTHORIZED
      }
    }

    "return BAD_GAYTEWAY" when {
      "a 'CLIENT_CLOSED_REQUEST' error response is recieved" in {
        val result = testPertaxAuthActionWith(EitherT.leftT(Future.successful(UpstreamErrorResponse("", CLIENT_CLOSED_REQUEST))))

        status(result) shouldBe BAD_GATEWAY
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "any other error response is recieved" in {
        val result = testPertaxAuthActionWith(EitherT.leftT(Future.successful(UpstreamErrorResponse("", IM_A_TEAPOT))))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private def mockPertaxConnectorWithResult[T](pertaxResult: EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse]): PertaxConnector = {
    val connector = mock[PertaxConnector]

    when(connector.authorise(any, any))
    .thenReturn(pertaxResult)

    connector
  }

  class PertaxAuthActionTestHarness(pertaxAuthAction: PertaxAuthAction) extends Controller {
    def call(): Action[AnyContent] = pertaxAuthAction { _ => Ok }
  }

  private def testPertaxAuthActionWith(pertaxResult: EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse]): Future[Result] = {
    val mockPertaxConnector = mockPertaxConnectorWithResult(pertaxResult)

    val injector = GuiceApplicationBuilder()
      .overrides(
        inject.bind[PertaxConnector].toInstance(mockPertaxConnector)
      )
      .injector()

    val pertaxAuthAction = injector.instanceOf[PertaxAuthAction]

    val testHarness = new PertaxAuthActionTestHarness(pertaxAuthAction)

    testHarness.call()(FakeRequest())
  }
}
