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

import org.joda.time.LocalDate
import org.mockito.{Matchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.NpsConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.nps._
import uk.gov.hmrc.nationalinsurancerecord.domain.{NationalInsuranceTaxYear, _}
import scala.concurrent.Future

class NationalInsuranceRecordServiceSpec extends NationalInsuranceRecordUnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar {
  // scalastyle:off magic.number

  private val niRecordHOD = NpsNIRecord(
    numberOfQualifyingYears = 36,
    nonQualifyingYears = 10,
    nonQualifyingYearsPayable = 4,
    dateOfEntry = Some(new LocalDate(1969,8,1)),
    pre75ContributionCount = 250,
    niTaxYears = List(
      NpsNITaxYear(
        startTaxYear = 2017,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2016,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2015,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2014,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 9,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = None,
        classOneContribution = 430.4,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2013,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
        classOneContribution = 0,
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(NpsOtherCredits(1,2,7))
      ),
      NpsNITaxYear(
        startTaxYear = 2012,
        qualifying = false,
        underInvestigation = false,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
        classOneContribution = 0,
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(NpsOtherCredits(1,2,7))
      )
    )
  )

  private val niRecordHODWithPre75QYears = NpsNIRecord(
    numberOfQualifyingYears = 6,
    nonQualifyingYears = 26,
    nonQualifyingYearsPayable = 3,
    dateOfEntry = Some(new LocalDate(1969,8,1)),
    pre75ContributionCount = 59,
    niTaxYears = List(
      NpsNITaxYear(
        startTaxYear = 2017,
        qualifying = false,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2016,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2015,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2014,
        qualifying = false,
        underInvestigation = false,
        payable = true,
        classThreePayable = 9,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = None,
        classOneContribution = 430.4,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2013,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
        classOneContribution = 0,
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(NpsOtherCredits(1,2,7))
      ),
        NpsNITaxYear(
        startTaxYear = 2012,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
        classOneContribution = 0,
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(NpsOtherCredits(1,2,7))
      )
    )
  )

  private val niRecordHODWithoutPre75QYears = NpsNIRecord(
    numberOfQualifyingYears = 6,
    nonQualifyingYears = 20,
    nonQualifyingYearsPayable = 3,
    dateOfEntry = Some(new LocalDate(1969,8,1)),
    pre75ContributionCount = 59,
    niTaxYears = List(
      NpsNITaxYear(
        startTaxYear = 2017,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),      NpsNITaxYear(
        startTaxYear = 2016,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2015,
        qualifying = true,
        underInvestigation = false,
        payable = false,
        classThreePayable = 0,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2014,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 9,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = None,
        classOneContribution = 430.4,
        classTwoCredits = 0,
        classThreeCredits = 0,
        otherCredits = List()
      ),
      NpsNITaxYear(
        startTaxYear = 2013,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
        classOneContribution = 0,
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(NpsOtherCredits(1,2,7))
      ),
        NpsNITaxYear(
        startTaxYear = 2012,
        qualifying = true,
        underInvestigation = false,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
        classOneContribution = 0,
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(NpsOtherCredits(1,2,7))
      )
    )
  )

  private val dummyTaxYearDefault: NationalInsuranceTaxYear = NationalInsuranceTaxYear(
    taxYear =  "2010-11",
    qualifying = false,
    classOneContributions =  1149.98,
    classTwoCredits =  0,
    classThreeCredits =  0,
    otherCredits =  0,
    classThreePayable = 0,
    classThreePayableBy = None,
    classThreePayableByPenalty = Some(new LocalDate(2023,4,5)),
    payable = false,
    underInvestigation =  false
  )

  "NationalInsuranceRecordService with a HOD Connection" when {

    val mockMetrics = mock[MetricsService]
    val service = new NpsConnection {
      override lazy val nps: NpsConnector = mock[NpsConnector]
      override lazy val citizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
      override lazy val now: LocalDate = new LocalDate(2017, 1, 16)
      override def metrics: MetricsService = mockMetrics
    }

    "regular ni record" should {

      val liabilities = NpsLiabilities(List(NpsLiability(14)))
      val nino = generateNino()

      when(service.citizenDetailsService.checkManualCorrespondenceIndicator(nino)).thenReturn(Future.successful(false))
      when(service.nps.getNationalInsuranceRecord(nino)).thenReturn(Future.successful(niRecordHOD))
      when(service.nps.getLiabilities(nino)).thenReturn(Future.successful(liabilities))
      when(service.nps.getSummary(nino)).thenReturn(Future.successful(NpsSummary(false, None, new LocalDate(2016, 4, 5), new LocalDate(1951, 4 , 5), 2017)))

      lazy val niRecordF: Future[NationalInsuranceRecord] = service.getNationalInsuranceRecord(nino).right.get
      lazy val niTaxYearF: Future[NationalInsuranceTaxYear] = service.getTaxYear(nino,TaxYear("2014-15")).right.get

      "return qualifying years to be 36" in {
        whenReady(niRecordF) { ni =>
          ni.qualifyingYears shouldBe 36
        }
      }
      // Theoretically its not possible, but want to test calcualtions
      "return qualifyingyears pre 1975 to be 31"  in {
        whenReady(niRecordF) { ni =>
          ni.qualifyingYearsPriorTo1975 shouldBe 31
        }
      }
      "return number of gaps to be 1"  in {
        whenReady(niRecordF) { ni =>
          ni.numberOfGaps shouldBe 1
        }
      }
      "return number of gaps payable to be 1"  in {
        whenReady(niRecordF) { ni =>
          ni.numberOfGapsPayable shouldBe 1
        }
      }
      "return date of entry to be 1969/8/1"  in {
        whenReady(niRecordF) { ni =>
          ni.dateOfEntry shouldBe Some(new LocalDate(1969,8,1))
        }
      }
      "return homeResponsibilities to be true"  in {
        whenReady(niRecordF) { ni =>
          ni.homeResponsibilitiesProtection shouldBe true
        }
      }
      "return earnings included upto to be 2016/8/1"  in {
        whenReady(niRecordF) { ni =>
          ni.earningsIncludedUpTo shouldBe new LocalDate(2016,4,5)
        }
      }
      "return taxYear to be 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.taxYear shouldBe "2017-18"
        }
      }
      "return qualifying status true for taxyear 2017-18 to be"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.qualifying shouldBe true
        }
      }
      "return classOneContributions to be 2430.24 for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classOneContributions shouldBe 2430.24
        }
      }
      "return classTwoCredits to be 0 for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classTwoCredits shouldBe 0
        }
      }
      "return classThreeCredits to be 0 for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreeCredits shouldBe 0
        }
      }
      "return otherCredits to be 0 for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.otherCredits shouldBe 0
        }
      }
      "return classThreePayable to be 0 for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreePayable shouldBe 0
          ni.taxYears(4).classThreePayable shouldBe 720
        }
      }
      "return classThreePayableBy to be None for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreePayableBy shouldBe None
          ni.taxYears(4).classThreePayableBy shouldBe Some(new LocalDate(2019,4,5))
        }
      }
      "return classThreePayableByPenalty to be None for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreePayableByPenalty shouldBe None
          ni.taxYears(4).classThreePayableByPenalty shouldBe Some(new LocalDate(2023,4,5))
        }
      }

      "return payable and underinvestigation flag to be false for taxyear 2017-18"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.payable shouldBe false
          ni.taxYears.head.underInvestigation shouldBe false
        }
      }
      "return tax Year details correctly" in {
        whenReady(niTaxYearF) {
          niTaxYear =>
            niTaxYear.taxYear shouldBe "2014-15"
            niTaxYear.underInvestigation shouldBe false
            niTaxYear.qualifying shouldBe true
            niTaxYear.payable shouldBe true
            niTaxYear.classThreePayable shouldBe 9
            niTaxYear.classThreePayableBy shouldBe Some(new LocalDate(2019,4,5))
            niTaxYear.classThreePayableByPenalty shouldBe None
            niTaxYear.classOneContributions shouldBe 430.4
            niTaxYear.classTwoCredits shouldBe 0
            niTaxYear.classThreeCredits shouldBe 0
            niTaxYear.otherCredits shouldBe 0
        }
      }
    }

    "calc pre75 years" should {
      "return 3 when the number of conts in 157 and the date of entry is 04/10/1972 and their date of birth is 04/10/1956" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, Some(new LocalDate(1972, 10, 4)), new LocalDate(1956, 10, 4)) shouldBe Some(3)
      }
      "return 8 when the number of conts in 408 and the date of entry is 08/01/1968 and their date of birth is 08/01/1952" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(408, Some(new LocalDate(1968, 1, 8)), new LocalDate(1952, 1, 8)) shouldBe Some(8)
      }
      "return 2 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 04/10/1956" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, Some(new LocalDate(1973, 4, 6)), new LocalDate(1956, 10, 4)) shouldBe Some(2)
      }
      "return 1 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 06/04/1958" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, Some(new LocalDate(1973, 4, 6)), new LocalDate(1958, 4, 6)) shouldBe Some(1)
      }
      "return null when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 24/05/1996" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, Some(new LocalDate(1973, 4, 6)), new LocalDate(1996, 5, 24)) shouldBe None
      }
      "return null when the number of conts in 157 and the date of entry is 06/04/1976 and their date of birth is 06/04/1960" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, Some(new LocalDate(1976, 4, 6)), new LocalDate(1960, 4, 6)) shouldBe None
      }
      "return null when the number of conts in 157 and the date of entry is 06/04/2005 and their date of birth is 06/04/1958" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, Some(new LocalDate(2005, 4, 6)), new LocalDate(1958, 4, 6)) shouldBe None
      }
      "when the date_of_entry is null, should still perform calc" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, None, new LocalDate(1922, 4, 6)) shouldBe Some(4)
      }
      "when the date_of_entry is null, should still restrict by 16th Birthday calc" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, None, new LocalDate(1957, 4, 6)) shouldBe Some(2)
      }
    }

    "calc pre75 qualifying years" should {
      "return pre-75 qualifying years = 2" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(niRecordHODWithPre75QYears) shouldBe Some(2)
      }

      "return nothing for post75 qualifying years" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(niRecordHODWithoutPre75QYears) shouldBe None
      }
    }

  }

  "NationalInsuranceRecordService exclusion with HOD connection" should {
    val mockMetrics = mock[MetricsService]
    val service = new NpsConnection {
      override lazy val nps: NpsConnector = mock[NpsConnector]
      override lazy val citizenDetailsService: CitizenDetailsService = {
        mock[CitizenDetailsService]
      }
      override lazy val now: LocalDate = {
        new LocalDate(2017, 1, 16)
      }

      override def metrics: MetricsService = mockMetrics
    }

    "NI Summary with exclusions" should {

      val summary = NpsSummary(
        rreToConsider = true,
        dateOfDeath = None,
        earningsIncludedUpTo = new LocalDate(1954, 4, 5),
        dateOfBirth = new LocalDate(1954, 7, 7),
        finalRelevantYear = 2049
      )
      val liabilities = NpsLiabilities(List(NpsLiability(14), NpsLiability(5)))
      val nino = generateNino()

      reset(mockMetrics)
      when(service.citizenDetailsService.checkManualCorrespondenceIndicator(nino)).thenReturn(Future.successful(false))
      when(service.nps.getNationalInsuranceRecord(nino)).thenReturn(Future.successful(niRecordHOD))
      when(service.nps.getLiabilities(nino)).thenReturn(Future.successful(liabilities))
      when(service.nps.getSummary(nino)).thenReturn(Future.successful(summary))

      lazy val niRecordF: Future[ExclusionResponse] = service.getNationalInsuranceRecord(nino).left.get

      "return Isle of Man exclusion" in {
        whenReady(niRecordF) { niExclusion =>
          niExclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
        }
      }

      "log exclusion in metrics" in {
        whenReady(niRecordF) { niExclusion =>
          verify(mockMetrics, Mockito.atLeastOnce()).exclusion(Matchers.eq(Exclusion.IsleOfMan))
        }
      }
    }
  }

}
