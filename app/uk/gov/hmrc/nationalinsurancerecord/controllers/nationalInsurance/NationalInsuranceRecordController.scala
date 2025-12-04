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

package uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance

import com.google.inject.{Inject, Singleton}
import play.api.hal.HalResource
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.CopeExclusionAction
import uk.gov.hmrc.nationalinsurancerecord.controllers.{ErrorHandling, ErrorResponses, HalSupport, Links}
import uk.gov.hmrc.nationalinsurancerecord.domain.*
import uk.gov.hmrc.nationalinsurancerecord.events.{NationalInsuranceExclusion, NationalInsuranceRecord}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService
import uk.gov.hmrc.nationalinsurancerecord.util.{ErrorResponseUtils, HeaderValidator}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
abstract class NationalInsuranceRecordController @Inject()(
  nationalInsuranceRecordService: NationalInsuranceRecordService,
  auditConnector: AuditConnector,
  appContext: AppContext,
  cc: ControllerComponents
) extends BackendController(cc)
    with HeaderValidator
    with ErrorHandling
    with HalSupport
    with Links {

  override val app: String = appContext.appName
  override val context: String = appContext.apiGatewayContext

  val authAction: ActionBuilder[Request, AnyContent]
  val copeAction: CopeExclusionAction
  implicit val ec: ExecutionContext

  private def halResourceWithTaxYears(nino: Nino, content: JsValue, selfLink: String, years: List[NationalInsuranceTaxYear]): HalResource = {
    halResourceSelfLink(
      content,
      selfLink,
      Some(Vector("taxYears" -> years.map(taxYear => halResourceSelfLink(Json.toJson(taxYear),
        nationalInsuranceTaxYearHref(nino, TaxYear(taxYear.taxYear)))).toVector))
    )
  }

  def getSummary(nino: Nino): Action[AnyContent] =
    (authAction andThen validateAccept(acceptHeaderValidationRules) andThen copeAction.filterCopeExclusions(nino)).async {
      implicit request =>
        nationalInsuranceRecordService.getNationalInsuranceRecord(nino) map {
          _.fold(handleDesError, {
            case NationalInsuranceRecordResult(Left(exclusion)) => handleExclusion(exclusion, nino,
              Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceRecordHref(nino))))
            case NationalInsuranceRecordResult(Right(nationalInsuranceRecord)) =>
              auditConnector.sendEvent(NationalInsuranceRecord(nino, nationalInsuranceRecord.qualifyingYears,
                nationalInsuranceRecord.qualifyingYearsPriorTo1975, nationalInsuranceRecord.numberOfGaps,
                nationalInsuranceRecord.numberOfGapsPayable, nationalInsuranceRecord.dateOfEntry,
                nationalInsuranceRecord.homeResponsibilitiesProtection, nationalInsuranceRecord.earningsIncludedUpTo,
                nationalInsuranceRecord.taxYears.length
              ))

              Ok(halResourceWithTaxYears(nino, Json.toJson(nationalInsuranceRecord), nationalInsuranceRecordHref(nino),
                years = nationalInsuranceRecord.taxYears))
          })
        }
    }


  def getTaxYear(nino: Nino, taxYear: TaxYear): Action[AnyContent] =
    (authAction andThen validateAccept(acceptHeaderValidationRules)).async {
      implicit request =>
        nationalInsuranceRecordService.getTaxYear(nino, taxYear) map {
          _.fold(handleDesError, {
            case NationalInsuranceTaxYearResult(Left(exclusion)) => handleExclusion(exclusion, nino,
              Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceTaxYearHref(nino, taxYear))))
            case NationalInsuranceTaxYearResult(Right(nationalInsuranceTaxYear)) =>
              import uk.gov.hmrc.nationalinsurancerecord.events.NationalInsuranceTaxYear
              auditConnector.sendEvent(NationalInsuranceTaxYear(nino, nationalInsuranceTaxYear.taxYear,
                nationalInsuranceTaxYear.qualifying, nationalInsuranceTaxYear.classOneContributions,
                nationalInsuranceTaxYear.classTwoCredits, nationalInsuranceTaxYear.classThreeCredits,
                nationalInsuranceTaxYear.otherCredits, nationalInsuranceTaxYear.classThreePayable,
                nationalInsuranceTaxYear.classThreePayableBy, nationalInsuranceTaxYear.classThreePayableByPenalty,
                nationalInsuranceTaxYear.payable, nationalInsuranceTaxYear.underInvestigation))

              Ok(halResourceSelfLink(Json.toJson(nationalInsuranceTaxYear), nationalInsuranceTaxYearHref(nino, taxYear)))
          })
        }
    }

  private def handleExclusion(exclusion: ExclusionResponse, nino: Nino, okResult: => Result)(implicit request: Request[AnyContent]): Result = {
    if (exclusion.exclusionReasons.contains(Exclusion.Dead)) {
      auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.Dead)))
      Forbidden(ErrorResponseUtils.convertToJson(ErrorResponses.ExclusionDead))
    } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
      auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
      Forbidden(ErrorResponseUtils.convertToJson(ErrorResponses.ExclusionManualCorrespondence))
    } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
      auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.IsleOfMan)))
      Forbidden(ErrorResponseUtils.convertToJson(ErrorResponses.ExclusionIsleOfMan))
    } else {
      auditConnector.sendEvent(NationalInsuranceExclusion(nino, exclusion.exclusionReasons))
      okResult
    }
  }
}