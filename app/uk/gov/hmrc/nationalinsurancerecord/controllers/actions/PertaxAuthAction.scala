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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.Results.{BadGateway, InternalServerError, Unauthorized}
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.nationalinsurancerecord.connectors.PertaxConnector

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject()(
                                      val pertaxConnector: PertaxConnector,
                                      val parser: BodyParsers.Default
                                    )(implicit val executionContext: ExecutionContext) extends PertaxAuthAction {

  private val CLIENT_CLOSED_REQUEST = 499

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    pertaxConnector.authorise
      .fold(
        {
          case UpstreamErrorResponse(_, UNAUTHORIZED, _, _) =>
            Some(Unauthorized(""))
          case UpstreamErrorResponse(_, CLIENT_CLOSED_REQUEST, _, _) =>
            Some(BadGateway("Dependant services failing"))
          case _ =>
            Some(InternalServerError("Unexpected response from pertax"))
        },
        {
          case PertaxAuthResponse("ACCESS_GRANTED", _) =>
            None
          case PertaxAuthResponse(code, message) =>
            Some(Unauthorized(s"Unauthorized - error code: $code message: $message"))
        }
      )
  }
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request]
