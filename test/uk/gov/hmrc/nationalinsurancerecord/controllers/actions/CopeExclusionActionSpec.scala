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

import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, Ok}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec

import scala.concurrent.Future
import scala.util.Random

abstract class CopeExclusionActionSpec extends NationalInsuranceRecordUnitSpec
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
  with ScalaFutures {

  val nino: Nino = generateNino()

  class TestHarness(action: CopeExclusionAction) extends BaseController {
    val controllerComponents: ControllerComponents = stubControllerComponents()
    def run(nino: Nino): Action[AnyContent] = action.filterCopeExclusions(nino) { _ => Ok }
  }

  def actionUnderTest: TestHarness

  def mockStatePensionConnector(nino: Nino, returnVal: Future[Option[HttpResponse]]): OngoingStubbing[Future[Option[HttpResponse]]]

  "CopeExclusionAction" should {
    "return Ok" when {

      "state pension connector returns None" in {
        mockStatePensionConnector(nino, Future.successful(None))
        actionUnderTest.run(nino)(FakeRequest()).futureValue shouldBe Ok
      }
    }

    "return Forbidden" when {

      "state pension connector returns Some" in {
        val body = s"""{ "body": "${Random.alphanumeric.take(1000).mkString}" }"""
        val response = HttpResponse(200, body)
        mockStatePensionConnector(nino, Future.successful(Some(response)))
        actionUnderTest.run(nino)(FakeRequest()).futureValue shouldBe Forbidden(Json.parse(body))
      }
    }

    "propagate failed future" in {
      val failure = new RuntimeException("FAILURE")
      mockStatePensionConnector(nino, Future.failed(failure))
      actionUnderTest.run(nino)(FakeRequest()).failed.futureValue shouldBe failure
    }
  }
}