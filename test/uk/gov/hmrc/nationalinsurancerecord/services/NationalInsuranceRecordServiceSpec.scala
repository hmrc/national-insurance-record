/*
 * Copyright 2016 HM Revenue & Customs
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
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.play.http.HeaderCarrier

class NationalInsuranceRecordServiceSpec extends NationalInsuranceRecordUnitSpec with OneAppPerSuite with ScalaFutures {

  private val dummyRecord: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 36,
    qualifyingYearsPriorTo1975 = 5,
    numberOfGaps = 10,
    numberOfGapsPayable = 4,
    dateOfEntry = new LocalDate(1969, 8, 1),
    homeResponsibilitiesProtection = false,
    List(
      TaxYearSummary("2015-16", true),
      TaxYearSummary("2014-15", true),
      TaxYearSummary("2013-14", true),
      TaxYearSummary("2012-13", false),
      TaxYearSummary("2011-12", false),
      TaxYearSummary("2010-11", false),
      TaxYearSummary("2009-10", true),
      TaxYearSummary("2008-09", false),
      TaxYearSummary("2007-08", true),
      TaxYearSummary("2006-07", true),
      TaxYearSummary("2005-06", true),
      TaxYearSummary("2004-05", true),
      TaxYearSummary("2003-04", true),
      TaxYearSummary("2002-03", true),
      TaxYearSummary("2001-02", false),
      TaxYearSummary("2000-01", true),
      TaxYearSummary("1999-00", true),
      TaxYearSummary("1998-99", true),
      TaxYearSummary("1997-98", true),
      TaxYearSummary("1996-97", false),
      TaxYearSummary("1995-96", false),
      TaxYearSummary("1994-95", true),
      TaxYearSummary("1993-94", true),
      TaxYearSummary("1992-93", true),
      TaxYearSummary("1991-92", true),
      TaxYearSummary("1990-91", true),
      TaxYearSummary("1989-90", true),
      TaxYearSummary("1988-89", true),
      TaxYearSummary("1987-88", true),
      TaxYearSummary("1986-87", false),
      TaxYearSummary("1985-86", false),
      TaxYearSummary("1984-85", true),
      TaxYearSummary("1983-84", false),
      TaxYearSummary("1982-83", true),
      TaxYearSummary("1981-82", true),
      TaxYearSummary("1980-81", true),
      TaxYearSummary("1979-80", true),
      TaxYearSummary("1978-79", true),
      TaxYearSummary("1977-78", true),
      TaxYearSummary("1976-77", true),
      TaxYearSummary("1975-76", true)
    )
  )
  
  private val dummyTaxYearDefault: TaxYear = TaxYear(
    taxYear =  "1989-90",
    qualifying = true,
    classOneContributions =  1149.98,
    classTwoCredits =  0,
    classThreeCredits =  0,
    otherCredits =  0,
    classThreePayable = 0,
    classThreePayableBy = None,
    classThreePayableByPenalty = None,
    payable = false,
    underInvestigation =  false
  )

  private val dummyTaxYear1: TaxYear = TaxYear(
    taxYear =  "1989-90",
    qualifying = true,
    classOneContributions =  1149.98,
    classTwoCredits =  0,
    classThreeCredits =  0,
    otherCredits =  0,
    classThreePayable = 0,
    classThreePayableBy = Some(new LocalDate(2017,4,5)),
    classThreePayableByPenalty = None,
    payable = false,
    underInvestigation =  false
  )

  "sandbox" should {

    "return ni record summary dummy data for non-existent prefix" in {
      val nino: Nino = generateNinoWithPrefix("ZX")
      whenReady(SandboxNationalInsuranceService.getNationalInsuranceRecord(nino)(HeaderCarrier())) { result =>
        result shouldBe Right(dummyRecord)
      }
    }

    "return tax year dummy data for non-existent prefix ZX" in {
      val nino: Nino = generateNinoWithPrefix("ZX")
      whenReady(SandboxNationalInsuranceService.getTaxYear(nino,"1989-90")(HeaderCarrier())) { result =>
        result shouldBe dummyTaxYearDefault
      }
    }

    "return tax year dummy data for existent prefix EY" in {
      val nino: Nino = generateNinoWithPrefix("EY")
      whenReady(SandboxNationalInsuranceService.getTaxYear(nino,"1989-90")(HeaderCarrier())) { result =>
        result shouldBe dummyTaxYear1
      }
    }

  }
}