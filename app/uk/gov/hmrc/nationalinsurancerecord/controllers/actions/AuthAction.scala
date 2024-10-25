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

import com.google.inject.ImplementedBy
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{clientId, nino, trustedHelper}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector, val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  private val logger = Logger(this.getClass)

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val matchNinoInUriPattern = "/ni/([^/]+)/?.*".r
    val matchNinoInMdtpUriPattern = "/ni/mdtp/([^/]+)/?.*".r

    val apiMatches = matchNinoInUriPattern.findAllIn(request.uri)
    val mdtpMatches = matchNinoInMdtpUriPattern.findAllIn(request.uri)

    def check(nino: String): Future[Option[Status]] = {
      val uriNino1 = if (apiMatches.nonEmpty) apiMatches.group(1) else ""
      val uriNino2 = if (mdtpMatches.nonEmpty) mdtpMatches.group(1) else ""
      if (uriNino1 == nino || uriNino2 == nino) successful(None)
      else {
        logger.warn("nino does not match nino in uri")
        successful(Some(Unauthorized))
      }
    }

    if (apiMatches.isEmpty && mdtpMatches.isEmpty) {
      successful(Some(BadRequest))
    } else {
      authorised(
        ConfidenceLevel.L200 or AuthProviders(PrivilegedApplication)
      ).retrieve(nino and trustedHelper and clientId) {
        case _ ~ _ ~ Some(_) => successful(None)
        case _ ~ Some(trusted) ~ _ => check(trusted.principalNino.getOrElse(""))
        case Some(nino) ~ None ~ _ => check(nino)
        case _ => successful(Some(Unauthorized))
      } recover {
        case e: AuthorisationException =>
          logger.info("Debug info - " + e.getMessage, e)
          Some(Unauthorized)
        case e: Throwable =>
          logger.error("Unexpected Error", e)
          Some(InternalServerError)
      }
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request]

