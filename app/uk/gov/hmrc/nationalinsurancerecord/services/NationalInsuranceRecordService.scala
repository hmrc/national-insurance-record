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

package uk.gov.hmrc.nationalinsurancerecord.services

import com.google.inject.Inject
import services.TaxYearResolver
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.nationalinsurancerecord.connectors.DesConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.domain.des._
import uk.gov.hmrc.nationalinsurancerecord.util.NIRecordConstants._

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceRecordService @Inject()(
                                                des: DesConnector,
                                                citizenDetailsService: CitizenDetailsService,
                                                metrics: MetricsService,
                                                implicit val executionContext: ExecutionContext
                                              ) {

  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[DesError, NationalInsuranceRecordResult]] =
    citizenDetailsService.checkManualCorrespondenceIndicator(nino) flatMap {
      mci =>
        for {
          niRecord <- des.getNationalInsuranceRecord(nino)
          liabilities <- des.getLiabilities(nino)
          summary <- des.getSummary(nino)
        } yield handleDesEither[NationalInsuranceRecordResult](niRecord, summary, liabilities,
          buildNationalInsuranceRecord(_: DesNIRecord, _: DesSummary, _: DesLiabilities, mci))
    }


  private def handleDesEither[A](niRecordEither: Either[DesError, DesNIRecord],
                                 desSummaryEither: Either[DesError, DesSummary],
                                 desLiabilitiesEither: Either[DesError, DesLiabilities],
                                 func: (DesNIRecord, DesSummary, DesLiabilities) => A
                                ): Either[DesError, A] = {
    (niRecordEither, desSummaryEither, desLiabilitiesEither) match {
      case (Right(niRecord), Right(desSummary), Right(desLiabilities)) => Right(func(niRecord, desSummary, desLiabilities))
      case (Left(niRecordError), _, _) => Left(niRecordError)
      case (_, Left(desSummaryError), _) => Left(desSummaryError)
      case (_, _, Left(desLiabilitiesError)) => Left(desLiabilitiesError)
    }
  }

  private def buildNationalInsuranceRecord(
                                            desNIRecord: DesNIRecord,
                                            desSummary: DesSummary,
                                            desLiabilities: DesLiabilities,
                                            manualCorrespondence: Boolean
                                          ): NationalInsuranceRecordResult = {
    val purgedNIRecord: DesNIRecord =
      desNIRecord.purge(desSummary.finalRelevantYear.get)

    val exclusions: List[Exclusion] =
      new DesExclusionService(
        dateOfDeath              = desSummary.dateOfDeath,
        liabilities              = desLiabilities.liabilities,
        manualCorrespondenceOnly = manualCorrespondence
      ).getExclusions

    if (exclusions.nonEmpty) {
      metrics.exclusion(exclusions.head)
      NationalInsuranceRecordResult(Left(ExclusionResponse(exclusions)))
    } else {
      val niRecord: NationalInsuranceRecord =
        NationalInsuranceRecord(
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

      metrics.niRecord(
        gaps            = niRecord.numberOfGaps,
        payableGaps     = niRecord.numberOfGapsPayable,
        pre75Years      = niRecord.qualifyingYearsPriorTo1975,
        qualifyingYears = niRecord.qualifyingYears
      )

      NationalInsuranceRecordResult(Right(niRecord))
    }
  }

  def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[DesError, NationalInsuranceTaxYearResult]] =
    citizenDetailsService.checkManualCorrespondenceIndicator(nino).flatMap {
      mci =>
        for {
          niRecord <- des.getNationalInsuranceRecord(nino)
          liabilities <- des.getLiabilities(nino)
          summary <- des.getSummary(nino)
        } yield handleDesEither[NationalInsuranceTaxYearResult](niRecord, summary, liabilities,
          buildTaxYear(_: DesNIRecord, _: DesSummary, _: DesLiabilities, mci, nino, taxYear))
    }


  private def buildTaxYear(
                            desNIRecord: DesNIRecord,
                            desSummary: DesSummary,
                            desLiabilities: DesLiabilities,
                            manualCorrespondence: Boolean,
                            nino: Nino,
                            taxYear: TaxYear
                          ): NationalInsuranceTaxYearResult = {
    val purgedNIRecord: DesNIRecord =
      desNIRecord.purge(desSummary.finalRelevantYear.get)

    val exclusions: List[Exclusion] =
      new DesExclusionService(
        dateOfDeath              = desSummary.dateOfDeath,
        liabilities              = desLiabilities.liabilities,
        manualCorrespondenceOnly = manualCorrespondence
      ).getExclusions

    if (exclusions.nonEmpty) {
      metrics.exclusion(exclusions.head)
      NationalInsuranceTaxYearResult(Left(ExclusionResponse(exclusions)))
    } else {
      purgedNIRecord
        .niTaxYears
        .map(desTaxYearToNIRecordTaxYear)
        .find(_.taxYear == taxYear.taxYear) match {
          case Some(nationalInsuranceRecordTaxYear) =>
            NationalInsuranceTaxYearResult(Right(nationalInsuranceRecordTaxYear))
          case _ =>
            throw new NotFoundException(s"taxYear ${taxYear.taxYear} Not Found for $nino")
        }
    }
  }


  private def desHomeResponsibilitiesProtection(liabilities: List[DesLiability]): Boolean =
    liabilities.exists(
      liability =>
        homeResponsibilitiesProtectionTypes.contains(liability.liabilityType.get)
    )

  def calcPre75QualifyingYears(pre75Contributions: Int, dateOfEntry: Option[LocalDate], dateOfBirth: LocalDate): Option[Int] = {
    val yearCalc: BigDecimal = BigDecimal(pre75Contributions) / 50
    val sixteenthBirthday: LocalDate = dateOfBirth.plusYears(niRecordMinAge)
    val sixteenthBirthdayDiff: Int = niRecordStart - TaxYearResolver.taxYearFor(sixteenthBirthday)

    val yearsPre75 = dateOfEntry match {
      case Some(doe) =>
        (niRecordStart - TaxYearResolver.taxYearFor(doe)).min(sixteenthBirthdayDiff)
      case None =>
        sixteenthBirthdayDiff
    }

    if (yearsPre75 > 0) {
      Some(yearCalc.setScale(0, BigDecimal.RoundingMode.CEILING).min(yearsPre75).toInt)
    } else {
      None
    }
  }

  private def desTaxYearToNIRecordTaxYear(desNITaxYear: DesNITaxYear): NationalInsuranceTaxYear =
    NationalInsuranceTaxYear(
      taxYear = desNITaxYear.taxYear,
      qualifying = desNITaxYear.qualifying,
      classOneContributions = desNITaxYear.classOneContribution,
      classTwoCredits = desNITaxYear.classTwoCredits,
      classThreeCredits = desNITaxYear.classThreeCredits,
      otherCredits = desNITaxYear.otherCredits.foldRight(0)(_.numberOfCredits.getOrElse(0) + _),
      classThreePayable = desNITaxYear.classThreePayable,
      classThreePayableBy = desNITaxYear.classThreePayableBy,
      classThreePayableByPenalty = desNITaxYear.classThreePayableByPenalty,
      payable = desNITaxYear.payable,
      underInvestigation = desNITaxYear.underInvestigation
    )
}
