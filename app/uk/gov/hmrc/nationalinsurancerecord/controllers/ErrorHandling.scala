/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorInternalServerError, ErrorNotFound}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadGatewayException, BadRequestException, GatewayTimeoutException, HeaderCarrier, NotFoundException, Upstream4xxResponse, Upstream5xxResponse }

trait ErrorHandling {
  self: BaseController =>

  val app: String

  def errorWrapper(func: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    func.recover {
      case e: NotFoundException =>
        NotFound(Json.toJson(ErrorNotFound))

      case e: GatewayTimeoutException => Logger.error(s"$app Gateway Timeout: ${e.getMessage}", e); GatewayTimeout
      case e: BadGatewayException => Logger.error(s"$app Bad Gateway: ${e.getMessage}", e); BadGateway
      case e: BadRequestException => BadRequest(Json.toJson(ErrorGenericBadRequest("Upstream Bad Request. Is this customer below State Pension Age?")))
      case e: Upstream4xxResponse => Logger.error(s"$app Upstream4XX: ${e.getMessage}", e); BadGateway
      case e: Upstream5xxResponse => Logger.error(s"$app Upstream5XX: ${e.getMessage}", e); BadGateway

      case e: Throwable =>
        Logger.error(s"$app Internal server error: ${e.getMessage}", e)
        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }
}
