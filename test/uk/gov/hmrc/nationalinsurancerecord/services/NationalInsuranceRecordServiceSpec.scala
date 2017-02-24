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
import org.mockito.Matchers
import uk.gov.hmrc.nationalinsurancerecord.connectors.NpsConnector

import scala.concurrent.Future

class NationalInsuranceRecordServiceSpec extends NationalInsuranceRecordUnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar {

  private val regularData: NationalInsuranceRecord = NationalInsuranceRecord(
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
          result shouldBe Right(regularData)
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
    }

    "regular ni record" should {

      val niRecord = NationalInsuranceRecord(
        qualifyingYears = 36,
        qualifyingYearsPriorTo1975 = 5,
        numberOfGaps = 10,
        numberOfGapsPayable = 4,
        dateOfEntry = new LocalDate(1969,8,1),
        homeResponsibilitiesProtection = false,
        earningsIncludedUpTo = new LocalDate(2016,4,5),
        List(
          NationalInsuranceTaxYear(
            taxYear = "2015-16",
            qualifying = true,
            classOneContributions = 2430.24,
            classTwoCredits = 0,
            classThreeCredits = 0,
            otherCredits = 0,
            classThreePayable = 0,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            payable = false,
            underInvestigation = false
          ),
          NationalInsuranceTaxYear(
            taxYear = "2014-15",
            qualifying = false,
            classOneContributions = 430.4,
            classTwoCredits = 0,
            classThreeCredits = 0,
            otherCredits = 0,
            classThreePayable = 9,
            classThreePayableBy = Some(new LocalDate(2019,4,5)),
            classThreePayableByPenalty = None,
            payable = true,
            underInvestigation = false
          ),
          NationalInsuranceTaxYear(
            taxYear = "2013-14",
            qualifying = true,
            classOneContributions = 0,
            classTwoCredits = 10,
            classThreeCredits = 3,
            otherCredits = 7,
            classThreePayable = 720,
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
            payable = true,
            underInvestigation = false
          )
        )
      )

      when(service.nps.getNationalInsuranceRecord).thenReturn(Future.successful(niRecord))

      lazy val niRecordF: Future[NationalInsuranceRecord] = service.getNationalInsuranceRecord(generateNino()).right.get

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
      "return number of gaps to be 10"  in {
        whenReady(niRecordF) { ni =>
          ni.numberOfGaps shouldBe 10
        }
      }
      "return number of gaps payable to be 4"  in {
        whenReady(niRecordF) { ni =>
          ni.numberOfGapsPayable shouldBe 4
        }
      }
      "return date of entry to be 1969/8/1"  in {
        whenReady(niRecordF) { ni =>
          ni.dateOfEntry shouldBe new LocalDate(1969,8,1)
        }
      }
      "return homeResponsibilities to be false"  in {
        whenReady(niRecordF) { ni =>
          ni.homeResponsibilitiesProtection shouldBe false
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

      "return payable and underinvestigateion flag to be false for taxyear 2015-16"  in {
        whenReady(niRecordF) { ni =>
          ni.taxYears.head.payable shouldBe false
          ni.taxYears.head.underInvestigation shouldBe false
        }
      }

    }
  }
}
