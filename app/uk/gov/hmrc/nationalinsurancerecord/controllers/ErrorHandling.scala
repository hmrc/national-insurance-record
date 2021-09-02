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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import play.api.Logger
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{BaseController, Result}
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound, ErrorResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ErrorHandling extends Logging {
  self: BaseController =>
  val app: String

  def errorWrapper(func: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    func.recover {
      case _: NotFoundException => NotFound(Json.toJson[ErrorResponse](ErrorNotFound))
      case e@WithStatusCode(GATEWAY_TIMEOUT) => logger.error(s"$app Gateway Timeout: ${e.getMessage}", e); GatewayTimeout
      case e: BadGatewayException => logger.error(s"$app Bad Gateway: ${e.getMessage}", e); BadGateway
      case _: BadRequestException => BadRequest(Json.toJson[ErrorResponse](ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?")))
      case e@UpstreamErrorResponse.Upstream4xxResponse(_) => logger.error(s"$app Upstream4XX: ${e.getMessage}", e); BadGateway
      case e@UpstreamErrorResponse.Upstream5xxResponse(_) => logger.error(s"$app Upstream5XX: ${e.getMessage}", e); BadGateway

      case e  : Throwable =>
        logger.error(s"$app Internal server error: ${e.getMessage}", e)
        InternalServerError(Json.toJson[ErrorResponse](ErrorInternalServerError))
    }
  }
}
