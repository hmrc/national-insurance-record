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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import com.google.inject.Inject
import play.api.hal.HalResource
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.connectors.CustomAuditConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.{Exclusion, TaxYear}
import uk.gov.hmrc.nationalinsurancerecord.events.{NationalInsuranceExclusion, NationalInsuranceRecord}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.nationalinsurancerecord.domain.NationalInsuranceTaxYear
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class NationalInsuranceRecordController @Inject()(nationalInsuranceRecordService: NationalInsuranceRecordService,
                                                  customAuditConnector: CustomAuditConnector) extends BaseController
  with HeaderValidator
  with ErrorHandling
  with HalSupport
  with Links {

  override val app: String = "National-Insurance-Record"
  override lazy val context: String = AppContext.apiGatewayContext

  private def halResourceWithTaxYears(nino: Nino, content: JsValue, selfLink: String, years: List[NationalInsuranceTaxYear]): HalResource = {
    halResourceSelfLink(
      content,
      selfLink,
      Some(Vector("taxYears" -> years.map(taxYear => halResourceSelfLink(Json.toJson(taxYear),
        nationalInsuranceTaxYearHref(nino, TaxYear(taxYear.taxYear)))).toVector))
    )
  }

  def getSummary(nino: Nino): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      errorWrapper(nationalInsuranceRecordService.getNationalInsuranceRecord(nino).map {

        case Left(exclusion) => {
          if (exclusion.exclusionReasons.contains(Exclusion.Dead)) {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.Dead)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionDead))
          } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionManualCorrespondence))
          } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.IsleOfMan)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionIsleOfMan))
          } else {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, exclusion.exclusionReasons))
            Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceRecordHref(nino)))
          }
        }

        case Right(nationalInsuranceRecord) =>
          customAuditConnector.sendEvent(NationalInsuranceRecord(nino, nationalInsuranceRecord.qualifyingYears,
            nationalInsuranceRecord.qualifyingYearsPriorTo1975, nationalInsuranceRecord.numberOfGaps,
            nationalInsuranceRecord.numberOfGapsPayable, nationalInsuranceRecord.dateOfEntry,
            nationalInsuranceRecord.homeResponsibilitiesProtection, nationalInsuranceRecord.earningsIncludedUpTo,
            nationalInsuranceRecord.taxYears.length
          ))

          Ok(halResourceWithTaxYears(nino, Json.toJson(nationalInsuranceRecord), nationalInsuranceRecordHref(nino),
            years = nationalInsuranceRecord.taxYears))
      })
  }

  def getTaxYear(nino: Nino, taxYear: TaxYear): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      errorWrapper(nationalInsuranceRecordService.getTaxYear(nino, taxYear).map {

        case Left(exclusion) =>
          if (exclusion.exclusionReasons.contains(Exclusion.Dead)) {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.Dead)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionDead))
          } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionManualCorrespondence))
          } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.IsleOfMan)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionIsleOfMan))
          } else {
            customAuditConnector.sendEvent(NationalInsuranceExclusion(nino, exclusion.exclusionReasons))
            Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceTaxYearHref(nino, taxYear)))
          }

        case Right(nationalInsuranceTaxYear) =>
          import uk.gov.hmrc.nationalinsurancerecord.events.NationalInsuranceTaxYear
          customAuditConnector.sendEvent(NationalInsuranceTaxYear(nino, nationalInsuranceTaxYear.taxYear,
            nationalInsuranceTaxYear.qualifying, nationalInsuranceTaxYear.classOneContributions,
            nationalInsuranceTaxYear.classTwoCredits, nationalInsuranceTaxYear.classThreeCredits,
            nationalInsuranceTaxYear.otherCredits, nationalInsuranceTaxYear.classThreePayable,
            nationalInsuranceTaxYear.classThreePayableBy, nationalInsuranceTaxYear.classThreePayableByPenalty,
            nationalInsuranceTaxYear.payable, nationalInsuranceTaxYear.underInvestigation))

          Ok(halResourceSelfLink(Json.toJson(nationalInsuranceTaxYear), nationalInsuranceTaxYearHref(nino, taxYear)))
      })

  }

}
