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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{BaseController, Result}
import uk.gov.hmrc.api.controllers.ErrorResponse
import uk.gov.hmrc.http.*
import uk.gov.hmrc.nationalinsurancerecord.controllers.ErrorResponses.*
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesError

import scala.concurrent.ExecutionContext

trait ErrorHandling extends Logging {
  self: BaseController =>
  val app: String
  implicit val executionContext: ExecutionContext

  protected def handleDesError(error: DesError): Result =
    error match {
      case DesError.HttpError(error) if error.statusCode == NOT_FOUND =>
        NotFound
      case DesError.HttpError(error) if error.statusCode == GATEWAY_TIMEOUT =>
        gatewayTimeout(error)
      case DesError.HttpError(error) if error.statusCode == BAD_REQUEST =>
        badRequest
      case DesError.HttpError(error) if error.statusCode == BAD_GATEWAY =>
        badGateway(error)
      case DesError.HttpError(UpstreamErrorResponse.Upstream4xxResponse(_)) =>
        upstream4xx(error)
      case DesError.HttpError(UpstreamErrorResponse.Upstream5xxResponse(_)) =>
        upstream5xx(error)
      case DesError.JsonValidationError(_) =>
        internalServerError(error)
      case DesError.OtherError(error) =>
        internalServerError(error)
      case value =>
        throw new NotImplementedError(s"Match not implemented for: $value")
    }

  private def gatewayTimeout(error: Throwable): Status = {
    logger.error(s"$app Gateway Timeout: ${error.getMessage}")
    GatewayTimeout
  }

  private def badRequest: Result = {
    logger.error("Upstream Bad Request. Is this customer below State Pension Age?")
    BadRequest
  }

  private def badGateway(error: Throwable): Status = {
    logger.error(s"$app Bad Gateway: ${error.getMessage}")
    BadGateway
  }

  private def internalServerError(error: Throwable): Result = {
    logger.error(s"$app Internal server error: ${error.getMessage}", error)
    InternalServerError(Json.toJson[ErrorResponse](ErrorInternalServerError))
  }

  private def upstream4xx(error: Throwable): Status = {
    logger.error(s"$app Upstream4XX: ${error.getMessage}", error)
    BadGateway
  }

  private def upstream5xx(error: Throwable): Status = {
    logger.error(s"$app Upstream5XX: ${error.getMessage}")
    BadGateway
  }
}
