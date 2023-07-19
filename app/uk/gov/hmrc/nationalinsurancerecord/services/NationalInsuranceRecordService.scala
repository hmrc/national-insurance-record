/*
 * Copyright 2023 HM Revenue & Customs
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
import services.TaxYearResolver
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.nationalinsurancerecord.connectors.DesConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesLiability, DesNITaxYear}
import uk.gov.hmrc.nationalinsurancerecord.util.NIRecordConstants

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceRecordService @Inject()(
  des: DesConnector,
  citizenDetailsService: CitizenDetailsService,
  metrics: MetricsService,
  implicit val executionContext: ExecutionContext
) {

  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] =

    for {
      desNIRecord          <- des.getNationalInsuranceRecord(nino)
      desLiabilities       <- des.getLiabilities(nino)
      desSummary           <- des.getSummary(nino)
      manualCorrespondence <- citizenDetailsService.checkManualCorrespondenceIndicator(nino)
    } yield {

      val purgedNIRecord = desNIRecord.purge(desSummary.finalRelevantYear.get)

      val exclusions: List[Exclusion] = new DesExclusionService(
        dateOfDeath = desSummary.dateOfDeath,
        desLiabilities.liabilities,
        manualCorrespondence
      ).getExclusions

      if (exclusions.nonEmpty) {
        metrics.exclusion(exclusions.head)
        Left(ExclusionResponse(exclusions))
      } else {
        val niRecord = NationalInsuranceRecord(
          purgedNIRecord.numberOfQualifyingYears,
          calcPre75QualifyingYears(purgedNIRecord.pre75ContributionCount, purgedNIRecord.dateOfEntry, desSummary.dateOfBirth.get).getOrElse(0),
          purgedNIRecord.nonQualifyingYears,
          purgedNIRecord.nonQualifyingYearsPayable,
          purgedNIRecord.dateOfEntry,
          desHomeResponsibilitiesProtection(desLiabilities.liabilities),
          desSummary.earningsIncludedUpTo.get,
          purgedNIRecord.niTaxYears.map(desTaxYearToNIRecordTaxYear).sortBy(_.taxYear)(Ordering[String].reverse),
          desSummary.rreToConsider
        )
        metrics.niRecord(niRecord.numberOfGaps, niRecord.numberOfGapsPayable, niRecord.qualifyingYearsPriorTo1975, niRecord.qualifyingYears)
        Right(niRecord)
      }
    }


  def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] =

    for {
      desNIRecord          <- des.getNationalInsuranceRecord(nino);
      desSummary           <- des.getSummary(nino);
      desLiabilities       <- des.getLiabilities(nino);
      manualCorrespondence <- citizenDetailsService.checkManualCorrespondenceIndicator(nino)
    } yield {

      val purgedNIRecord = desNIRecord.purge(desSummary.finalRelevantYear.get)

      val exclusions: List[Exclusion] = new DesExclusionService(
        desSummary.dateOfDeath,
        desLiabilities.liabilities,
        manualCorrespondence
      ).getExclusions

      if (exclusions.nonEmpty) {
        metrics.exclusion(exclusions.head)
        Left(ExclusionResponse(exclusions))
      } else {
        purgedNIRecord.niTaxYears.map(desTaxYearToNIRecordTaxYear).find(_.taxYear == taxYear.taxYear) match {
          case Some(nationalInsuranceRecordTaxYear) =>
            Right(nationalInsuranceRecordTaxYear)
          case _ =>
            throw new NotFoundException(s"taxYear ${taxYear.taxYear} Not Found for $nino")
        }
      }
    }

  private def desHomeResponsibilitiesProtection(liabilities: List[DesLiability]): Boolean =
    liabilities.exists(
      liability =>
        NIRecordConstants.homeResponsibilitiesProtectionTypes.contains(liability.liabilityType.get)
    )

  def calcPre75QualifyingYears(pre75Contributions: Int, dateOfEntry: Option[LocalDate], dateOfBirth: LocalDate): Option[Int] = {
    val yearCalc: BigDecimal = BigDecimal(pre75Contributions)/50
    val sixteenthBirthday: LocalDate = dateOfBirth.plusYears(NIRecordConstants.niRecordMinAge)
    val sixteenthBirthdayDiff: Int = NIRecordConstants.niRecordStart - TaxYearResolver.taxYearFor(sixteenthBirthday)
    val yearsPre75 = dateOfEntry match {
      case Some(doe) =>
        (NIRecordConstants.niRecordStart - TaxYearResolver.taxYearFor(doe)).min(sixteenthBirthdayDiff)
      case None =>
        sixteenthBirthdayDiff
    }
    if (yearsPre75 > 0) {
      Some(yearCalc.setScale(0, BigDecimal.RoundingMode.CEILING).min(yearsPre75).toInt)
    } else {
      None
    }
  }

  private def desTaxYearToNIRecordTaxYear(desNITaxYear: DesNITaxYear): NationalInsuranceTaxYear = {
    NationalInsuranceTaxYear(
      desNITaxYear.taxYear,
      desNITaxYear.qualifying,
      desNITaxYear.classOneContribution,
      desNITaxYear.classTwoCredits,
      desNITaxYear.classThreeCredits,
      desNITaxYear.otherCredits.foldRight(0)(_.numberOfCredits.getOrElse(0) + _),
      desNITaxYear.classThreePayable,
      desNITaxYear.classThreePayableBy,
      desNITaxYear.classThreePayableByPenalty,
      desNITaxYear.payable,
      desNITaxYear.underInvestigation
    )
  }
}
