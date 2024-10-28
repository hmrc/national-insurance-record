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

package uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance

import com.google.inject.Inject
import play.api.mvc.{ActionBuilder, AnyContent, BodyParsers, ControllerComponents, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.{ApiAuthAction, CopeExclusionAction}
import uk.gov.hmrc.nationalinsurancerecord.domain.TaxYear
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class ApiNationalInsuranceRecordController @Inject() (
                                                       action: ApiAuthAction,
                                                       nationalInsuranceRecordService: NationalInsuranceRecordService,
                                                       auditConnector: AuditConnector,
                                                       appContext: AppContext,
                                                       copeAction: CopeExclusionAction,
                                                       cc: ControllerComponents,
                                                       val parser: BodyParsers.Default,
                                                       val executionContext: ExecutionContext
)(override implicit val ec: ExecutionContext)
  extends NationalInsuranceRecordController(nationalInsuranceRecordService, auditConnector, appContext, copeAction, cc) {
  override val authAction: ActionBuilder[Request, AnyContent] = action

  override def endpointSummaryUrl(nino: Nino): String =
    uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance.routes.ApiNationalInsuranceRecordController.getSummary(nino).url

  override def endpointTaxYearUrl(nino: Nino, taxYear: TaxYear): String =
    uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance.routes.ApiNationalInsuranceRecordController.getTaxYear(nino, taxYear).url
}
