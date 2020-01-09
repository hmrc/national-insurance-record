/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.controllers.auth

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationalinsurancerecord.config.WSHttp
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector)(implicit executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    val matchNinoInUriPattern = "/ni/([^/]+)/?.*".r

    val matches = matchNinoInUriPattern.findAllIn(request.uri)

    if (matches.isEmpty) {
      Future.successful(Some(BadRequest))
    } else {
      val uriNino: Option[String] = Some(matches.group(1))
      authorised((AuthProviders(PrivilegedApplication)) or (ConfidenceLevel.L200 and Nino(hasNino = true, uriNino))) {
        Future.successful(None)
      }.recover {
        case t: Throwable =>
          Logger.debug("Debug info - " + t.getMessage)
          Some(Unauthorized)
      }
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request] with ActionFilter[Request]

class AuthConnector @Inject()(val http: WSHttp,
                              val runModeConfiguration: Configuration,
                              environment: Environment
                             ) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")

  override protected def mode: Mode = environment.mode
}
