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

package uk.gov.hmrc.nationalinsurancerecord.services

import com.google.inject.Inject
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationalinsurancerecord.connectors.FandFConnector

import scala.concurrent.{ExecutionContext, Future}

class FandFService @Inject()(fandFConnector: FandFConnector) {

  def getTrustedHelperNino(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    fandFConnector.getTrustedHelper.map {
      case Some(th) =>
        th.principalNino match {
          case Some(nino) =>
            Some(nino)
          case _ =>
            None
        }
      case _ =>
        None
    }

}
