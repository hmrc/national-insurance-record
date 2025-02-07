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

package uk.gov.hmrc.nationalinsurancerecord.util

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.nationalinsurancerecord.util.ErrorResponseUtils._
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class ErrorResponseUtilsSpec extends AnyWordSpec with Matchers {

  "convertToJson" should {
    "return a JSON object with correct structure" in {
      val errorResponse = ErrorResponse(NOT_FOUND, "Error not found", Some("NOT_FOUND"))
      val json: JsValue = convertToJson(errorResponse)

      json mustBe Json.obj(
 "code" -> "NOT_FOUND",
        "message" -> "Error not found"
      )
    }

    "handle cases where xStatusCode is None" in {
      val errorResponse = ErrorResponse(NOT_FOUND, "Error not found", None)
      val json: JsValue = convertToJson(errorResponse)

      json mustBe Json.obj(
        "code" -> "No status code provided",
        "message" -> "Error not found"
      )
    }
  }

  "errorResponseToResult" should {
    "return a Result with the correct status code and json body" in {
      val errorResponse = ErrorResponse(NOT_FOUND, "Error not found", Some("NOT_FOUND"))
      val result: Future[Result] = Future.successful(errorResponseToResult(errorResponse))

      status(result) mustBe NOT_FOUND

      val jsonBody = contentAsJson(result)

      jsonBody mustBe Json.obj(
        "code" -> "NOT_FOUND",
        "message" -> "Error not found"
      )
    }
  }
}
