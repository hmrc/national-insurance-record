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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import play.api.hal.HalResource
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.connectors.CustomAuditConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.{Exclusion, TaxYear, TaxYearSummary}
import uk.gov.hmrc.nationalinsurancerecord.events.{NationalInsuranceRecord, NationalInsuranceRecordExclusion}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.nationalinsurancerecord.domain.NationalInsuranceTaxYear
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

trait NationalInsuranceRecordController extends BaseController with HeaderValidator with ErrorHandling with HalSupport with Links {
  val nationalInsuranceRecordService: NationalInsuranceRecordService
  val customAuditConnector: CustomAuditConnector

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
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.Dead)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionDead))
          } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionManualCorrespondence))
          } else if (exclusion.exclusionReasons.contains(Exclusion.MarriedWomenReducedRateElection)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.MarriedWomenReducedRateElection)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionMarriedWomenReducedRate))
          } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.IsleOfMan)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionIsleOfMan))
          } else {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, exclusion.exclusionReasons))
            Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceRecordHref(nino)))
          }
        }

        case Right(nationalInsuranceRecord) =>
          customAuditConnector.sendEvent(NationalInsuranceRecord(nino, nationalInsuranceRecord.qualifyingYears,
                nationalInsuranceRecord.qualifyingYearsPriorTo1975, nationalInsuranceRecord.numberOfGaps,
                nationalInsuranceRecord.numberOfGapsPayable, nationalInsuranceRecord.dateOfEntry,
                nationalInsuranceRecord.homeResponsibilitiesProtection
              )
          )

          Ok(halResourceWithTaxYears(nino, Json.toJson(nationalInsuranceRecord), nationalInsuranceRecordHref(nino),
            years = nationalInsuranceRecord.taxYears))
      })
  }

  def getTaxYear(nino: Nino, taxYear: TaxYear): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      errorWrapper(nationalInsuranceRecordService.getTaxYear(nino, taxYear).map {

        case Left(exclusion) =>
          if (exclusion.exclusionReasons.contains(Exclusion.Dead)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.Dead)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionDead))
          } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionManualCorrespondence))
          } else if (exclusion.exclusionReasons.contains(Exclusion.MarriedWomenReducedRateElection)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.MarriedWomenReducedRateElection)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionMarriedWomenReducedRate))
          } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, List(Exclusion.IsleOfMan)))
            Forbidden(Json.toJson(ErrorResponses.ExclusionIsleOfMan))
          } else {
            customAuditConnector.sendEvent(NationalInsuranceRecordExclusion(nino, exclusion.exclusionReasons))
            Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceTaxYearHref(nino,taxYear)))
          }

        case Right(nationalInsuranceTaxYear) =>
          import uk.gov.hmrc.nationalinsurancerecord.events.NationalInsuranceTaxYear
          customAuditConnector.sendEvent(NationalInsuranceTaxYear(nino, nationalInsuranceTaxYear.taxYear,
              nationalInsuranceTaxYear.qualifying, nationalInsuranceTaxYear.classOneContributions,
              nationalInsuranceTaxYear.classTwoCredits, nationalInsuranceTaxYear.classThreeCredits,
              nationalInsuranceTaxYear.otherCredits, nationalInsuranceTaxYear.classThreePayable,
              nationalInsuranceTaxYear.classThreePayableBy, nationalInsuranceTaxYear.classThreePayableByPenalty,
              nationalInsuranceTaxYear.payable, nationalInsuranceTaxYear.underInvestigation))

          Ok(halResourceSelfLink(Json.toJson(nationalInsuranceTaxYear), nationalInsuranceTaxYearHref(nino,taxYear)))
      })

  }

}
