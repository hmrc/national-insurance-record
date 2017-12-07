/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.controllers.live

import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.connectors.CustomAuditConnector
import uk.gov.hmrc.nationalinsurancerecord.controllers.NationalInsuranceRecordController
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService


object NationalInsuranceRecordController extends NationalInsuranceRecordController {
  override val nationalInsuranceRecordService: NationalInsuranceRecordService = NationalInsuranceRecordService
  override val app: String = "National-Insurance-Record"
  override val context: String = AppContext.apiGatewayContext
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
}
