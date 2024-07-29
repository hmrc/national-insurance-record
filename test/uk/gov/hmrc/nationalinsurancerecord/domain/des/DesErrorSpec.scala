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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nationalinsurancerecord.util.UnitSpec

class DesErrorSpec extends UnitSpec {

  "DesError" when {
    "using .getMessage" should {
      "get the error message of a validation error" in {
        val validationError = DesError.JsonValidationError("Error Message")

        validationError.getMessage shouldBe "Error Message"
      }
      "get the error message of a http error" in {
        val httpError = DesError.HttpError(UpstreamErrorResponse.apply("Error Message", NOT_FOUND))

        httpError.getMessage shouldBe "Error Message"
      }
      "get the error message of any other error" in {

        val exceptionError = DesError.OtherError(new RuntimeException("Error Message"))

        exceptionError.getMessage shouldBe "Error Message"
      }
    }
  }
}
