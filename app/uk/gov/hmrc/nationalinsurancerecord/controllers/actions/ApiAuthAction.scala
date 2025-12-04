/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.clientId
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class ApiAuthActionImpl @Inject()(
                                   cc: ControllerComponents,
                                   val authConn: AuthConnector,
                                   val parse: BodyParsers.Default
                                 )(implicit val ec: ExecutionContext)
  extends ApiAuthAction with AuthorisedFunctions with Logging {

  val predicate: Predicate = AuthProviders(PrivilegedApplication)

  override def authConnector: AuthConnector = authConn

  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(predicate).retrieve(clientId) {
      case Some(_) =>
        Future.successful(None)
      case _ =>
        Future.successful(Some(Unauthorized))
    } recover {
      case e: AuthorisationException =>
        logger.info("Debug info - " + e.getMessage, e)
        Some(Unauthorized)
      case e: Throwable =>
        logger.error("Unexpected Error", e)
        Some(InternalServerError)
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}


@ImplementedBy(classOf[ApiAuthActionImpl])
trait ApiAuthAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request]