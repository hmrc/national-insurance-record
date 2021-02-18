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

package uk.gov.hmrc.nationalinsurancerecord.services

import com.google.inject.Inject
import play.api.http.Status.LOCKED
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationalinsurancerecord.connectors.CitizenDetailsConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CitizenDetailsService @Inject()(citizenDetailsConnector: CitizenDetailsConnector) {

  def checkManualCorrespondenceIndicator(nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] = {
    citizenDetailsConnector.retrieveMCIStatus(nino).map(status => status == LOCKED)
  }

}
