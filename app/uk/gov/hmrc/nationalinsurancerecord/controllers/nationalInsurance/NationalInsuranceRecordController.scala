/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, BodyParsers, ControllerComponents}
import uk.gov.hmrc.api.controllers.{ErrorResponse, HeaderValidator}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.{AuthAction, CopeExclusionAction}
import uk.gov.hmrc.nationalinsurancerecord.controllers.{ErrorHandling, ErrorResponses, HalSupport, Links}
import uk.gov.hmrc.nationalinsurancerecord.domain.{Exclusion, NationalInsuranceTaxYear, TaxYear}
import uk.gov.hmrc.nationalinsurancerecord.events.{NationalInsuranceExclusion, NationalInsuranceRecord}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class NationalInsuranceRecordController @Inject()(nationalInsuranceRecordService: NationalInsuranceRecordService,
                                                  auditConnector: AuditConnector,
                                                  appContext: AppContext,
                                                  authAction: AuthAction,
                                                  copeAction: CopeExclusionAction,
                                                  cc: ControllerComponents,
                                                  val parser: BodyParsers.Default
                                                 )(implicit val executionContext: ExecutionContext)
                                                  extends BackendController(cc)
                                                    with HeaderValidator
                                                    with ErrorHandling
                                                    with HalSupport
                                                    with Links {

  override val app: String = appContext.appName
  override val context: String = appContext.apiGatewayContext

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
        errorWrapper(nationalInsuranceRecordService.getNationalInsuranceRecord(nino).map {

          case Left(exclusion) => {
            if (exclusion.exclusionReasons.contains(Exclusion.Dead)) {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.Dead)))
              Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionDead))
            } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
              Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionManualCorrespondence))
            } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.IsleOfMan)))
              Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionIsleOfMan))
            } else {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, exclusion.exclusionReasons))
              Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceRecordHref(nino)))
            }
          }

          case Right(nationalInsuranceRecord) =>
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

  def getTaxYear(nino: Nino, taxYear: TaxYear): Action[AnyContent] =
    (authAction andThen validateAccept(acceptHeaderValidationRules)).async {
      implicit request =>
        errorWrapper(nationalInsuranceRecordService.getTaxYear(nino, taxYear).map {

          case Left(exclusion) =>
            if (exclusion.exclusionReasons.contains(Exclusion.Dead)) {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.Dead)))
              Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionDead))
            } else if (exclusion.exclusionReasons.contains(Exclusion.ManualCorrespondenceIndicator)) {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.ManualCorrespondenceIndicator)))
              Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionManualCorrespondence))
            } else if (exclusion.exclusionReasons.contains(Exclusion.IsleOfMan)) {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, List(Exclusion.IsleOfMan)))
              Forbidden(Json.toJson[ErrorResponse](ErrorResponses.ExclusionIsleOfMan))
            } else {
              auditConnector.sendEvent(NationalInsuranceExclusion(nino, exclusion.exclusionReasons))
              Ok(halResourceSelfLink(Json.toJson(exclusion), nationalInsuranceTaxYearHref(nino, taxYear)))
            }

          case Right(nationalInsuranceTaxYear) =>
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
