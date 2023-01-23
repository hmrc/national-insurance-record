/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.util.WireMockHelper

class StatePensionConnectorSpec extends NationalInsuranceRecordUnitSpec with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.state-pension.port" -> server.port()
    )
    .build()

  lazy val connector: StatePensionConnector = app.injector.instanceOf[StatePensionConnector]

  val nino = generateNino()

  def stubEndpoint(rtnStatus: Int, body: String): StubMapping = {
    server.stubFor(get(urlEqualTo(s"/cope/$nino"))
      .withHeader("accept", equalTo("application/vnd.hmrc.1.0+json"))
      .willReturn(
        aResponse()
          .withStatus(rtnStatus)
          .withHeader("Content-Type", "application/json")
          .withBody(body)
      )
    )
  }

  "StatePensionConnector" should {
    "return None for 404" in {
      stubEndpoint(NOT_FOUND, "{}")

      connector.getCopeCase(nino).futureValue shouldBe None
    }

    "return Some(response) for 403 (cope processing)" in {
      val json = """{
                   |    "errorCode": "EXCLUSION_COPE_PROCESSING",
                   |    "copeDataAvailableDate": "2021-07-16"
                   |}""".stripMargin

      stubEndpoint(FORBIDDEN, json)

      connector.getCopeCase(nino).futureValue.map(_.status) shouldBe Some(FORBIDDEN)
      connector.getCopeCase(nino).futureValue.map(_.body) shouldBe Some(json)
    }

    "return Some(response) for 403 (cope failed)" in {
      val json = """{
                   |    "errorCode": "EXCLUSION_COPE_PROCESSING_FAILED"
                   |}""".stripMargin

      stubEndpoint(FORBIDDEN, json)

      connector.getCopeCase(nino).futureValue.map(_.status) shouldBe Some(FORBIDDEN)
      connector.getCopeCase(nino).futureValue.map(_.body) shouldBe Some(json)
    }

    "return failed future for other response types" in {
      stubEndpoint(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")

      connector.getCopeCase(nino).failed.futureValue shouldBe UpstreamErrorResponse("INTERNAL_SERVER_ERROR", INTERNAL_SERVER_ERROR)
    }
  }
}
