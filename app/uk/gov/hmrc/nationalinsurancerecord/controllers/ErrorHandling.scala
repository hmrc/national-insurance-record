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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{BaseController, Result}
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound, ErrorResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesError

import scala.concurrent.{ExecutionContext, Future}

trait ErrorHandling extends Logging {
  self: BaseController =>
  val app: String
  implicit val executionContext: ExecutionContext

  def errorWrapper(func: => Future[Result]): Future[Result] =
    func.recover {
      case error => handleLegacyError(error)
    }

  protected def handleDesError(error: DesError): Result =
    error match {
      case DesError.HttpError(error) if error.statusCode == NOT_FOUND =>
        notFound
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

  private def handleLegacyError(error: Throwable): Result =
    error match {
      case _: NotFoundException =>
        notFound
      case _@WithStatusCode(GATEWAY_TIMEOUT) =>
        gatewayTimeout(error)
      case _: BadGatewayException =>
        badGateway(error)
      case _: BadRequestException =>
        badRequest
      case _@UpstreamErrorResponse.Upstream4xxResponse(_) =>
        upstream4xx(error)
      case _@UpstreamErrorResponse.Upstream5xxResponse(_) =>
        upstream5xx(error)
      case _: Throwable =>
        internalServerError(error)
    }

  private def notFound: Result =
    NotFound(Json.toJson[ErrorResponse](ErrorNotFound))

  private def gatewayTimeout(error: Throwable): Status = {
    logger.error(s"$app Gateway Timeout: ${error.getMessage}", error)
    GatewayTimeout
  }

  private def badRequest: Result =
    BadRequest(
      Json.toJson[ErrorResponse](
        ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?")
      )
    )

  private def badGateway(error: Throwable): Status = {
    logger.error(s"$app Bad Gateway: ${error.getMessage}", error)
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
    logger.error(s"$app Upstream5XX: ${error.getMessage}", error)
    BadGateway
  }
}
