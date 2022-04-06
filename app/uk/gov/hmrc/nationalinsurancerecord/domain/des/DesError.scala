/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.http.UpstreamErrorResponse

sealed trait DesError extends Exception

object DesError {
  case class JsonValidationError(message: String) extends DesError {
    override def getMessage: String = message
  }

  case class HttpError(error: UpstreamErrorResponse) extends DesError {
    override def getMessage: String = error.message
  }

  case class OtherError(error: Throwable) extends DesError {
    override def getMessage: String = error.getMessage
  }
}
