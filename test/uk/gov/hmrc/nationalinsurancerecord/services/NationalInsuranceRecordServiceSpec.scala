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

package uk.gov.hmrc.nationalinsurancerecord.services

import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.{NationalInsuranceTaxYear, _}
import uk.gov.hmrc.play.http.HeaderCarrier
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.nationalinsurancerecord.connectors.NpsConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.nps._

import scala.concurrent.Future

class NationalInsuranceRecordServiceSpec extends NationalInsuranceRecordUnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar {
  // scalastyle:off magic.number

  private val niRecordHOD = NpsNIRecord(
    numberOfQualifyingYears = 36,
    nonQualifyingYears = 10,
    nonQualifyingYearsPayable = 4,
    dateOfEntry = new LocalDate(1969,8,1),
    pre75ContributionCount = 250,
    niTaxYears = List(
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
      )
    )
  )

  private val regularDataSandbox: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 36,
    qualifyingYearsPriorTo1975 = 5,
    numberOfGaps = 10,
    numberOfGapsPayable = 4,
    dateOfEntry = new LocalDate(1969, 8, 1),
    homeResponsibilitiesProtection = false,
    earningsIncludedUpTo = new LocalDate(2016, 4, 5),
    List(
      NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("2014-15", true, 2430.24, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("2013-14", true, 2430.24, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("2012-13", false, 0, 0, 0, 12, 530, Some(new LocalDate(2019, 4, 5)), Some(new LocalDate(2023, 4, 5)), true, false),
      NationalInsuranceTaxYear("2011-12", false, 0, 0, 0, 0, 655.2, Some(new LocalDate(2019, 4, 5)), Some(new LocalDate(2023, 4, 5)), true, false),
      NationalInsuranceTaxYear("2010-11", false, 1149.98, 0, 0, 0, 0, None, Some(new LocalDate(2023, 4, 5)), false, false),
      NationalInsuranceTaxYear("2009-10", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("2008-09", false, 0, 0, 0, 1, 675.75, Some(new LocalDate(2019, 4, 5)), Some(new LocalDate(2023, 4, 5)), true, false),
      NationalInsuranceTaxYear("2007-08", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("2006-07", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("2005-06", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("2004-05", true, 2779.48, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("2003-04", true, 0, 0, 0, 53, 0, None, None, false, false),
      NationalInsuranceTaxYear("2002-03", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("2001-02", false, 14.3, 0, 0, 7, 0.0, None, Some(new LocalDate(2008, 4, 5)), true, true),
      NationalInsuranceTaxYear("2000-01", true, 1111.95, 0, 0, 2, 0, None, None, false, false),
      NationalInsuranceTaxYear("1999-00", true, 860.99, 0, 0, 1, 0, None, None, false, false),
      NationalInsuranceTaxYear("1998-99", true, 311.44, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1997-98", true, 2094.04, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1996-97", false, 0, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1995-96", false, 0, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1994-95", true, 2094.04, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1993-94", true, 996.32, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1992-93", true, 1555.84, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1991-92", true, 1085.76, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1990-91", true, 840.84, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1989-90", true, 1149.98, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("1988-89", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("1987-88", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("1986-87", false, 36.04, 0, 0, 3, 0, None, None, false, false),
      NationalInsuranceTaxYear("1985-86", false, 0, 0, 0, 34, 0, None, None, false, false),
      NationalInsuranceTaxYear("1984-85", true, 239.21, 0, 0, 19, 0, None, None, false, false),
      NationalInsuranceTaxYear("1983-84", false, 17.76, 0, 0, 44, 0, None, None, false, false),
      NationalInsuranceTaxYear("1982-83", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("1981-82", true, 0, 0, 0, 52, 0, None, None, false, false),
      NationalInsuranceTaxYear("1980-81", true, 114.19, 0, 0, 35, 0, None, None, false, false),
      NationalInsuranceTaxYear("1979-80", true, 24.9, 0, 0, 42, 0, None, None, false, false),
      NationalInsuranceTaxYear("1978-79", true, 69.35, 0, 0, 41, 0, None, None, false, false),
      NationalInsuranceTaxYear("1977-78", true, 82.13, 0, 0, 28, 0, None, None, false, false),
      NationalInsuranceTaxYear("1976-77", true, 53.5, 0, 0, 34, 0, None, None, false, false),
      NationalInsuranceTaxYear("1975-76", true, 70.67, 0, 0, 6, 0, None, None, false, false)
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

  "Sandbox getNationalInsuranceRecord" when {

    "nino input is a non-existent prefix" should {

      "return the default dummy data" in {
        val nino: Nino = generateNinoWithPrefix("ZX")
        whenReady(SandboxNationalInsuranceService.getNationalInsuranceRecord(nino)(HeaderCarrier())) { result =>
          result shouldBe Right(regularDataSandbox)
        }
      }
    }

    "nino input is MA" should {
      "return the Isle of Man exclusion" in {
        val nino: Nino = generateNinoWithPrefix("MA")
        whenReady(SandboxNationalInsuranceService.getNationalInsuranceRecord(nino)(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.IsleOfMan)))
        }
      }
    }

    "nino input is MW" should {
      "return the Married Women exclusion" in {
        val nino: Nino = generateNinoWithPrefix("MW")
        whenReady(SandboxNationalInsuranceService.getNationalInsuranceRecord(nino)(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.MarriedWomenReducedRateElection)))
        }
      }
    }

    "nino input is EZ" should {
      "return the dead user exclusion" in {
        val nino: Nino = generateNinoWithPrefix("EZ")
        whenReady(SandboxNationalInsuranceService.getNationalInsuranceRecord(nino)(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.Dead)))
        }
      }
    }

    "nino input is PG" should {
      "return the manual correspondence exclusion" in {
        val nino: Nino = generateNinoWithPrefix("PG")
        whenReady(SandboxNationalInsuranceService.getNationalInsuranceRecord(nino)(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator)))
        }
      }
    }
  }

  "Sandbox getTaxYear" when {

    "nino input is a non-existent prefix" should {

      "return the default dummy data" in {
        val nino: Nino = generateNinoWithPrefix("ZX")
        whenReady(SandboxNationalInsuranceService.getTaxYear(nino, TaxYear("2010-11"))(HeaderCarrier())) { result =>
          result shouldBe Right(dummyTaxYearDefault)
        }
      }
    }

    "nino input is MA" should {
      "return the Isle of Man exclusion" in {
        val nino: Nino = generateNinoWithPrefix("MA")
        whenReady(SandboxNationalInsuranceService.getTaxYear(nino, TaxYear("1900-01"))(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.IsleOfMan)))
        }
      }
    }

    "nino input is MW" should {
      "return the Married Women exclusion" in {
        val nino: Nino = generateNinoWithPrefix("MW")
        whenReady(SandboxNationalInsuranceService.getTaxYear(nino, TaxYear("1900-01"))(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.MarriedWomenReducedRateElection)))
        }
      }
    }

    "nino input is EZ" should {
      "return the dead user exclusion" in {
        val nino: Nino = generateNinoWithPrefix("EZ")
        whenReady(SandboxNationalInsuranceService.getTaxYear(nino, TaxYear("1900-01"))(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.Dead)))
        }
      }
    }

    "nino input is PG" should {
      "return the manual correspondence exclusion" in {
        val nino: Nino = generateNinoWithPrefix("PG")
        whenReady(SandboxNationalInsuranceService.getTaxYear(nino, TaxYear("1900-01"))(HeaderCarrier())) { result =>
          result shouldBe Left(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator)))
        }
      }
    }
  }


  "NationalInsuranceRecordService with a HOD Connection" when {

    val service = new NpsConnection {
      override lazy val nps: NpsConnector = mock[NpsConnector]
      override lazy val citizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
      override lazy val now: LocalDate = new LocalDate(2017, 1, 16)
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
      "return qualifyingyears pre 1975 to be 5"  in {
        whenReady(niRecordF) { ni =>
          ni.qualifyingYearsPriorTo1975 shouldBe 5
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
          ni.dateOfEntry shouldBe new LocalDate(1969,8,1)
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
      "return taxYear to be 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.taxYear shouldBe "2015-16"
        }
      }
      "return qualifying status true for taxyear 2015-16 to be"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.qualifying shouldBe true
        }
      }
      "return classOneContributions to be 2430.24 for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classOneContributions shouldBe 2430.24
        }
      }
      "return classTwoCredits to be 0 for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classTwoCredits shouldBe 0
        }
      }
      "return classThreeCredits to be 0 for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreeCredits shouldBe 0
        }
      }
      "return otherCredits to be 0 for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.otherCredits shouldBe 0
        }
      }
      "return classThreePayable to be 0 for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreePayable shouldBe 0
          ni.taxYears(2).classThreePayable shouldBe 720
        }
      }
      "return classThreePayableBy to be None for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreePayableBy shouldBe None
          ni.taxYears(2).classThreePayableBy shouldBe Some(new LocalDate(2019,4,5))
        }
      }
      "return classThreePayableByPenalty to be None for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.classThreePayableByPenalty shouldBe None
          ni.taxYears(2).classThreePayableByPenalty shouldBe Some(new LocalDate(2023,4,5))
        }
      }

      "return payable and underinvestigation flag to be false for taxyear 2015-16"  in {
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
            niTaxYear.qualifying shouldBe false
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
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, new LocalDate(1972, 10, 4), new LocalDate(1956, 10, 4)) shouldBe Some(3)
      }
      "return 8 when the number of conts in 408 and the date of entry is 08/01/1968 and their date of birth is 08/01/1952" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(408, new LocalDate(1968, 1, 8), new LocalDate(1952, 1, 8)) shouldBe Some(8)
      }
      "return 2 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 04/10/1956" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, new LocalDate(1973, 4, 6), new LocalDate(1956, 10, 4)) shouldBe Some(2)
      }
      "return 1 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 06/04/1958" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, new LocalDate(1973, 4, 6), new LocalDate(1958, 4, 6)) shouldBe Some(1)
      }
      "return 3 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 24/05/1996" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, new LocalDate(1973, 4, 6), new LocalDate(1996, 5, 24)) shouldBe None
      }
      "return 3 when the number of conts in 157 and the date of entry is 06/04/1976 and their date of birth is 06/04/1960" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, new LocalDate(1976, 4, 6), new LocalDate(1960, 4, 6)) shouldBe None
      }
      "return 3 when the number of conts in 157 and the date of entry is 06/04/2005 and their date of birth is 06/04/1958" in {
        NationalInsuranceRecordService.calcPre75QualifyingYears(157, new LocalDate(2005, 4, 6), new LocalDate(1958, 4, 6)) shouldBe None
      }
    }
  }

  "NationalInsuranceRecordService exclusion with HOD connection" should {
    val service = new NpsConnection {
      override lazy val nps: NpsConnector = mock[NpsConnector]
      override lazy val citizenDetailsService: CitizenDetailsService = {
        mock[CitizenDetailsService]
      }
      override lazy val now: LocalDate = {
        new LocalDate(2017, 1, 16)
      }
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

      when(service.citizenDetailsService.checkManualCorrespondenceIndicator(nino)).thenReturn(Future.successful(false))
      when(service.nps.getNationalInsuranceRecord(nino)).thenReturn(Future.successful(niRecordHOD))
      when(service.nps.getLiabilities(nino)).thenReturn(Future.successful(liabilities))
      when(service.nps.getSummary(nino)).thenReturn(Future.successful(summary))

      lazy val niRecordF: Future[ExclusionResponse] = service.getNationalInsuranceRecord(nino).left.get

      "return Isle of Man and married women reduced rate election exclusion" in {
        whenReady(niRecordF) { niExclusion =>
          niExclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan, Exclusion.MarriedWomenReducedRateElection)
        }
      }
    }
  }

}
