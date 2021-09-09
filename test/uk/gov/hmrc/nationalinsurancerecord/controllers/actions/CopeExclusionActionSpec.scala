/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, Ok}
import play.api.mvc.{Action, AnyContent, BaseController}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.StatePensionConnector

import scala.concurrent.Future
import scala.util.Random

class CopeExclusionActionSpec extends NationalInsuranceRecordUnitSpec
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
  with ScalaFutures {

  val nino = generateNino()

  class TestHarness(action: CopeExclusionAction) extends BaseController {
    val controllerComponents = stubControllerComponents()
    def run(nino: Nino): Action[AnyContent] = action.filterCopeExclusions(nino) { _ => Ok }
  }

  lazy val actionUnderTest = new TestHarness(app.injector.instanceOf[CopeExclusionAction])
  val mockStatePensionConnector = mock[StatePensionConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[StatePensionConnector].toInstance(mockStatePensionConnector)
    )
    .build()

  override def beforeEach = {
    reset(mockStatePensionConnector)
  }

  "CopeExclusionAction" should {
    "return Ok" when {

      "state pension connector returns None" in {
        when(mockStatePensionConnector.getCopeCase(meq(nino))(any())).thenReturn(Future.successful(None))
        actionUnderTest.run(nino)(FakeRequest()).futureValue shouldBe Ok
      }
    }

    "return Forbidden" when {

      "state pension connector returns Some" in {
        val body = s"""{ "body": "${Random.alphanumeric.take(1000).mkString}" }"""
        val response = HttpResponse(200, body)
        when(mockStatePensionConnector.getCopeCase(meq(nino))(any())).thenReturn(Future.successful(Some(response)))
        actionUnderTest.run(nino)(FakeRequest()).futureValue shouldBe Forbidden(Json.parse(body))
      }
    }

    "propagate failed future" in {
      val failure = new RuntimeException("FAILURE")
      when(mockStatePensionConnector.getCopeCase(meq(nino))(any())).thenReturn(Future.failed(failure))
      actionUnderTest.run(nino)(FakeRequest()).failed.futureValue shouldBe failure
    }
  }
}