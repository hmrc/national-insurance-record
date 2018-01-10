/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.TimeZone
import org.joda.time.{DateTimeZone, LocalDate}
import uk.gov.hmrc.domain.Nino
import play.api.Play.current
import uk.gov.hmrc.nationalinsurancerecord.connectors.NpsConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.domain.nps.{NpsLiability, NpsNITaxYear}
import uk.gov.hmrc.nationalinsurancerecord.util.NIRecordConstants
import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.http.{ HeaderCarrier, NotFoundException }

trait NationalInsuranceRecordService {
  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]]
  def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]]
}

trait NpsConnection extends NationalInsuranceRecordService {

  def nps: NpsConnector
  def citizenDetailsService: CitizenDetailsService
  def now: LocalDate
  def metrics: MetricsService

  override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] = {

    val npsNIRecordF = nps.getNationalInsuranceRecord(nino)
    val npsLiabilitiesF = nps.getLiabilities(nino)
    val npsSummaryF = nps.getSummary(nino)
    val manualCorrespondenceF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)

    for(
      npsNIRecord <- npsNIRecordF;
      npsLiabilities <- npsLiabilitiesF;
      npsSummary <- npsSummaryF;
      manualCorrespondence <- manualCorrespondenceF
    ) yield {

      val purgedNIRecord = npsNIRecord.purge(npsSummary.finalRelevantYear)

        val exclusions: List[Exclusion] = new ExclusionService(
          dateOfDeath = npsSummary.dateOfDeath,
          npsLiabilities.liabilities,
          manualCorrespondence
        ).getExclusions

        if(exclusions.nonEmpty) {
          metrics.exclusion(exclusions.head)
          Left(ExclusionResponse(exclusions))
        } else {
          val niRecord = NationalInsuranceRecord(
            purgedNIRecord.numberOfQualifyingYears,
            calcPre75QualifyingYears(purgedNIRecord.pre75ContributionCount, purgedNIRecord.dateOfEntry, npsSummary.dateOfBirth).getOrElse(0),
            purgedNIRecord.nonQualifyingYears,
            purgedNIRecord.nonQualifyingYearsPayable,
            purgedNIRecord.dateOfEntry,
            homeResponsibilitiesProtection(npsLiabilities.liabilities),
            npsSummary.earningsIncludedUpTo,
            purgedNIRecord.niTaxYears.map(npsTaxYearToNIRecordTaxYear).sortBy(_.taxYear)(Ordering[String].reverse),
            npsSummary.rreToConsider
          )
          metrics.niRecord(niRecord.numberOfGaps, niRecord.numberOfGapsPayable, niRecord.qualifyingYearsPriorTo1975, niRecord.qualifyingYears)
          Right(niRecord)
        }
      }
  }

  override def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] = {
    val npsNIRecordF = nps.getNationalInsuranceRecord(nino)
    val npsSummaryF = nps.getSummary(nino)
    val npsLiabilitiesF = nps.getLiabilities(nino)
    val manualCorrespondenceIndicatorF = citizenDetailsService.checkManualCorrespondenceIndicator(nino)

    for (
      npsNIRecord <- npsNIRecordF;
      npsSummary <- npsSummaryF;
      npsLiabilities <- npsLiabilitiesF;
      manualCorrespondenceIndicator <- manualCorrespondenceIndicatorF
    ) yield {

      val purgedNIRecord = npsNIRecord.purge(npsSummary.finalRelevantYear)

      val exclusions = new ExclusionService(
        npsSummary.dateOfDeath,
        npsLiabilities.liabilities,
        manualCorrespondenceIndicator
      ).getExclusions

      if (exclusions.nonEmpty) {
        metrics.exclusion(exclusions.head)
        Left(ExclusionResponse(exclusions))
      } else {
        purgedNIRecord.niTaxYears.map(npsTaxYearToNIRecordTaxYear).find(x => x.taxYear == taxYear.taxYear) match {
          case Some(nationalInsuranceRecordTaxYear) => Right(nationalInsuranceRecordTaxYear)
          case _ => throw new NotFoundException(s"taxYear ${taxYear.taxYear} Not Found for $nino")
        }
      }
    }
  }

  def homeResponsibilitiesProtection(liabilities: List[NpsLiability]): Boolean =
    liabilities.exists(liability => NIRecordConstants.homeResponsibilitiesProtectionTypes.contains(liability.liabilityType))

  def calcPre75QualifyingYears(pre75Contributions: Int, dateOfEntry: Option[LocalDate], dateOfBirth: LocalDate): Option[Int] = {
    val yearCalc: BigDecimal = BigDecimal(pre75Contributions)/50
    val sixteenthBirthday: LocalDate = new LocalDate(dateOfBirth.plusYears(NIRecordConstants.niRecordMinAge))
    val sixteenthBirthdayDiff: Int = NIRecordConstants.niRecordStart - TaxYearResolver.taxYearFor(sixteenthBirthday)
    val yearsPre75 = dateOfEntry match {
      case Some(doe) => (NIRecordConstants.niRecordStart - TaxYearResolver.taxYearFor(doe)).min(sixteenthBirthdayDiff)
      case None => sixteenthBirthdayDiff
    }
    if (yearsPre75 > 0) {
      Some(yearCalc.setScale(0, BigDecimal.RoundingMode.CEILING).min(yearsPre75).toInt)
    } else {
      None
    }
  }

  def npsTaxYearToNIRecordTaxYear(npsNITaxYear: NpsNITaxYear): NationalInsuranceTaxYear = {
    NationalInsuranceTaxYear(
      npsNITaxYear.taxYear,
      npsNITaxYear.qualifying,
      npsNITaxYear.classOneContribution,
      npsNITaxYear.classTwoCredits,
      npsNITaxYear.classThreeCredits,
      npsNITaxYear.otherCredits.foldRight(0)(_.numberOfCredits + _),
      npsNITaxYear.classThreePayable,
      npsNITaxYear.classThreePayableBy,
      npsNITaxYear.classThreePayableByPenalty,
      npsNITaxYear.payable,
      npsNITaxYear.underInvestigation
    )
  }

}

object NationalInsuranceRecordService extends NationalInsuranceRecordService with NpsConnection {
  override lazy val nps: NpsConnector = NpsConnector
  override def citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override def now: LocalDate = LocalDate.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/London")))
  override def metrics: MetricsService = MetricsService
}
