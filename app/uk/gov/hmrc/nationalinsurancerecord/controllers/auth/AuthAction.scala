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

package uk.gov.hmrc.nationalinsurancerecord.controllers.auth

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, PlayAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.nationalinsurancerecord.config.WSHttp
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector)(implicit executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {
  override protected def refine[A](request: Request[A]): Future[Either[Result, Request[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    authorised(ConfidenceLevel.L200) {
      Future.successful(Right(request))
    }.recover {
      case t: Throwable => {
        Logger.debug("Debug info - " + t.getMessage)
        Left(Unauthorized)
      }
    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request] with ActionRefiner[Request, Request]

class AuthConnector @Inject()(val http: WSHttp,
                              val runModeConfiguration: Configuration,
                              environment: Environment
                                         ) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")
  override protected def mode: Mode = environment.mode
}
