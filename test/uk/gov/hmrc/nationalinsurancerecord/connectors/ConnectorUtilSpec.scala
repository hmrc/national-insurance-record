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

package uk.gov.hmrc.nationalinsurancerecord.connectors

import org.scalatest.Inside.inside
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{BAD_GATEWAY, NOT_FOUND, OK}
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesError, DesLiabilities, DesLiability, LiabilityType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConnectorUtilSpec extends NationalInsuranceRecordUnitSpec with ScalaFutures {
  val connectorUtil = new ConnectorUtil()

  "handleConnectorResponse" should {
    "return a successful response" when {
      "the futureResponse is validated successfully" in {
        val body =
          s"""
            |{
            |    "liabilities": [
            |        {
            |            "liabilityType": ${LiabilityType.ISLE_OF_MAN}
            |        }
            |    ]
            |}
        """.stripMargin

        val futureResponse = Future.successful(Right(HttpResponse(OK, body)))
        await(connectorUtil.handleConnectorResponse[DesLiabilities](futureResponse, "Error")) shouldBe
          Right(DesLiabilities(List(DesLiability(Some(LiabilityType.ISLE_OF_MAN)))))
      }
    }
    "return a failed response" when {
      "there is an error in the json validation" in {
        val body =
          s"""
             |{
             |    "liabilities": [ {
             |        "liabilityType": "String"
             |        }
             |    ]
             |}
        """.stripMargin

        val futureResponse = Future.successful(Right(HttpResponse(OK, body)))
        val result = await(connectorUtil.handleConnectorResponse[DesLiabilities](futureResponse, "Error"))

        inside(result) {
          case Left(jsonValidationError) => jsonValidationError shouldBe a[DesError.JsonValidationError]
        }
      }
      "a http error is returned" in {
        val futureResponse = Future.successful(Left(UpstreamErrorResponse("Error", NOT_FOUND)))
        await(connectorUtil.handleConnectorResponse[DesLiabilities](futureResponse, "Error")) shouldBe
          Left(DesError.HttpError(UpstreamErrorResponse("Error", NOT_FOUND)))
      }
      "a http exception is returned" in {
        val futureResponse = Future.failed(new HttpException("Error", BAD_GATEWAY))
        connectorUtil.handleConnectorResponse[DesLiabilities](futureResponse, "Error").futureValue shouldBe
          Left(DesError.HttpError(UpstreamErrorResponse("Error", BAD_GATEWAY)))
      }
      "another exception is returned" in {
        val error = new RuntimeException("Error")
        val futureResponse = Future.failed(error)
        connectorUtil.handleConnectorResponse[DesLiabilities](futureResponse, "Error").futureValue shouldBe
          Left(DesError.OtherError(error))
      }
    }
  }
}
