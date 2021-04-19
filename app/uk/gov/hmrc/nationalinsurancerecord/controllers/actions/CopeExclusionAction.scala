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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.connectors.StatePensionConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

class CopeExclusionAction @Inject()(
  statePensionConnector: StatePensionConnector,
  defaultBodyParser: BodyParsers.Default
)(implicit ec: ExecutionContext)
  extends Results with BackendHeaderCarrierProvider {

  def filterCopeExclusions(nino: Nino) =
    new ActionBuilder[Request, AnyContent] with ActionFilter[Request] {
      override def parser: BodyParser[AnyContent] = defaultBodyParser
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
        statePensionConnector
          .getCopeCase(nino)(hc(request))
          .map {
            _.map { copeCase =>
              Forbidden(Json.parse(copeCase.body))
            }
          }
      }
    }
}