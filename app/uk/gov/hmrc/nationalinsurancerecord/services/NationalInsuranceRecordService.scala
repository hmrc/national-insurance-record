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
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nationalinsurancerecord.connectors.{DesConnector, ProxyCacheConnector}
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.domain.des._
import uk.gov.hmrc.nationalinsurancerecord.util.NIRecordConstants._

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceRecordService @Inject()(
  des: DesConnector,
  proxyCacheConnector: ProxyCacheConnector,
  citizenDetailsService: CitizenDetailsService,
  metrics: MetricsService,
  implicit val executionContext: ExecutionContext,
  featureFlagService: FeatureFlagService
) {

  //TODO may need t hide the Eithers under a Type
  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[DesError, Either[ExclusionResponse, NationalInsuranceRecord]]] =
    featureFlagService.get(ProxyCacheToggle) flatMap {
      proxyCache =>
        citizenDetailsService.checkManualCorrespondenceIndicator(nino) flatMap {
          mci =>
            if (proxyCache.isEnabled) {
              proxyCacheConnector.get(nino) map {
                pcd =>
                  buildNationalInsuranceRecord(pcd.niRecord, pcd.summary, pcd.liabilities, mci) match {
                    case Right(niRecord) => Right(Right(niRecord))
                    case Left(exclusion) => Right(Left(exclusion))
                  }
              }
            } else {
              for {
                niRecord    <- des.getNationalInsuranceRecord(nino)
                liabilities <- des.getLiabilities(nino)
                summary     <- des.getSummary(nino)
              } yield buildNationalInsuranceRecord(niRecord, summary, liabilities, mci)
            }
        }
    }

  private def buildNationalInsuranceRecord(niRecordEither: Either[DesError, DesNIRecord],
                                           desSummaryEither: Either[DesError, DesSummary],
                                           desLiabilitiesEither: Either[DesError, DesLiabilities],
                                           manualCorrespondence: Boolean
                                          ): Either[DesError, Either[ExclusionResponse, NationalInsuranceRecord]] = {

    (niRecordEither, desSummaryEither, desLiabilitiesEither) match {
      case (Right(niRecord), Right(desSummary), Right(desLiabilities)) =>
        buildNationalInsuranceRecord(niRecord, desSummary, desLiabilities, manualCorrespondence) match {
          case Right(niRecord) => Right(Right(niRecord))
          case Left(exclusion) => Right(Left(exclusion))
        }
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
  ): Either[ExclusionResponse, NationalInsuranceRecord] = {
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
      Left(ExclusionResponse(exclusions))
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

      Right(niRecord)
    }
  }

  def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[DesError, Either[ExclusionResponse, NationalInsuranceTaxYear]]] =
    featureFlagService.get(ProxyCacheToggle).flatMap {
      proxyCache =>
        citizenDetailsService.checkManualCorrespondenceIndicator(nino).flatMap {
          mci =>
            if (proxyCache.isEnabled) {
              proxyCacheConnector.get(nino) map {
                pcd =>
                  buildTaxYear(pcd.niRecord, pcd.summary, pcd.liabilities, mci, nino, taxYear) match {
                    case Right(niTaxYear) => Right(Right(niTaxYear))
                    case Left(exclusion) => Right(Left(exclusion))
                  }
              }
            } else {
              for {
                niRecord    <- des.getNationalInsuranceRecord(nino)
                liabilities <- des.getLiabilities(nino)
                summary     <- des.getSummary(nino)
              } yield buildTaxYear(niRecord, summary, liabilities, mci, nino, taxYear)
            }
        }
    }


  private def buildTaxYear(
    desNIRecord: DesNIRecord,
    desSummary: DesSummary,
    desLiabilities: DesLiabilities,
    manualCorrespondence: Boolean,
    nino: Nino,
    taxYear: TaxYear
  ): Either[ExclusionResponse, NationalInsuranceTaxYear] = {
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
      Left(ExclusionResponse(exclusions))
    } else {
      purgedNIRecord
        .niTaxYears
        .map(desTaxYearToNIRecordTaxYear)
        .find(_.taxYear == taxYear.taxYear) match {
          case Some(nationalInsuranceRecordTaxYear) =>
            Right(nationalInsuranceRecordTaxYear)
          case _ =>
            throw new NotFoundException(s"taxYear ${taxYear.taxYear} Not Found for $nino")
        }
    }
  }

  private def buildTaxYear(niRecordEither: Either[DesError, DesNIRecord],
                           desSummaryEither: Either[DesError, DesSummary],
                           desLiabilitiesEither: Either[DesError, DesLiabilities],
                           manualCorrespondence: Boolean,
                           nino: Nino,
                           taxYear: TaxYear
                          ): Either[DesError, Either[ExclusionResponse, NationalInsuranceTaxYear]] = {
    (niRecordEither, desSummaryEither, desLiabilitiesEither) match {
      case (Right(niRecord), Right(desSummary), Right(desLiabilities)) =>
        buildTaxYear(niRecord, desSummary, desLiabilities, manualCorrespondence, nino, taxYear) match {
          case Right(niTaxRecord) => Right(Right(niTaxRecord))
          case Left(exclusion) => Right(Left(exclusion))
        }
      case (Left(niRecordError), _, _) => Left(niRecordError)
      case (_, Left(desSummaryError), _) => Left(desSummaryError)
      case (_, _, Left(desLiabilitiesError)) => Left(desLiabilitiesError)
    }
  }

  private def desHomeResponsibilitiesProtection(liabilities: List[DesLiability]): Boolean =
    liabilities.exists(
      liability =>
        homeResponsibilitiesProtectionTypes.contains(liability.liabilityType.get)
    )

  def calcPre75QualifyingYears(pre75Contributions: Int, dateOfEntry: Option[LocalDate], dateOfBirth: LocalDate): Option[Int] = {
    val yearCalc: BigDecimal = BigDecimal(pre75Contributions)/50
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
