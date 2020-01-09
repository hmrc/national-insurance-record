/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

import scala.io.Source

class DesNIRecordSpec extends UnitSpec {

  val testRecord: String = Source.fromInputStream(getClass.getResourceAsStream("/json/nirecordDesTest.json")).mkString

  "reading DesNIRecord " should {
    val niRecord = Json.parse(testRecord).as[DesNIRecord]

    "parse number of Qualifying year correctly" in {
      niRecord.numberOfQualifyingYears shouldBe 37
    }
    "parse number of non-qualifying year correctly" in {
      niRecord.nonQualifyingYears shouldBe 2
    }
    "parse number of non qualifying payable years correctly" in {
      niRecord.nonQualifyingYearsPayable shouldBe 2
    }
    "parse number pre-75 year contribution count correctly" in {
      niRecord.pre75ContributionCount shouldBe 0
    }
    "parse date of entry correctly" in {
      niRecord.dateOfEntry shouldBe Some(new LocalDate(1986, 9, 8))
      niRecord.niTaxYears.head.qualifying shouldBe true
    }
    "parse qualifying status of tax year correctly" in {
      niRecord.niTaxYears(1).qualifying shouldBe true
    }
    "parse underInvestigation status of tax year correctly" in {
      niRecord.niTaxYears.head.underInvestigation shouldBe false
      niRecord.niTaxYears(1).underInvestigation shouldBe false
    }
    "parse payable status of tax year correctly" in {
      niRecord.niTaxYears.head.payable shouldBe false
      niRecord.niTaxYears(1).payable shouldBe false
    }
    "parse class three payable amount for qualifying and non-qualifying tax year correctly" in {
      niRecord.niTaxYears.head.classThreePayable shouldBe 0
      niRecord.niTaxYears(30).classThreePayable shouldBe 733.20
    }
    "parse class three payable by date correctly" in {
      niRecord.niTaxYears(30).classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
      niRecord.niTaxYears.head.classThreePayableByPenalty shouldBe None
    }
    "parse class three payable by penalty date correctly" in {
      niRecord.niTaxYears(30).classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
    }
    "parse niEarningsEmployed (ClassOneContribution) correctly" in {
      niRecord.niTaxYears.head.classOneContribution shouldBe 416.99
      niRecord.niTaxYears(1).classOneContribution shouldBe 466.85
    }
    "parse niEarningsSelfEmployed (ClassTwoCredits) correctly" in {
      niRecord.niTaxYears.head.classTwoCredits shouldBe 0
    }
    "parse otherCredits values correctly" in {
      niRecord.niTaxYears(17).otherCredits.head.creditContributionType shouldBe Some(24)
      niRecord.niTaxYears(17).otherCredits.head.creditSourceType shouldBe Some(47)
      niRecord.niTaxYears(17).otherCredits.head.numberOfCredits shouldBe Some(53)
      niRecord.niTaxYears(17).otherCredits shouldBe List(DesOtherCredits(Some(24), Some(47), Some(53)))
    }
  }

  def taxYear(year: Int, qualifying: Boolean, payable: Boolean): DesNITaxYear =
    DesNITaxYear(year, qualifying, underInvestigation = false, payable = payable, 0, None, None, 0, 0, 0, List())

  "purge" should {
    "return an nirecord with no tax years after 2014 when the FRY 2014" in {
      val niRecord = DesNIRecord(5, 2, 2, pre75ContributionCount = 0, Some(new LocalDate(2010, 4, 6)), List(
        taxYear(2010, true, false),
        taxYear(2011, true, false),
        taxYear(2012, true, false),
        taxYear(2013, true, false),
        taxYear(2014, true, false),
        taxYear(2015, false, true),
        taxYear(2016, false, true)
      ))

      val purged = niRecord.purge(finalRelevantStartYear = 2014)

      purged.numberOfQualifyingYears shouldBe 5
      purged.nonQualifyingYears shouldBe 0
      purged.nonQualifyingYearsPayable shouldBe 0
      purged.pre75ContributionCount shouldBe 0
      purged.dateOfEntry shouldBe Some(new LocalDate(2010, 4, 6))
      purged.niTaxYears shouldBe List(
        taxYear(2010, true, false),
        taxYear(2011, true, false),
        taxYear(2012, true, false),
        taxYear(2013, true, false),
        taxYear(2014, true, false)
      )
    }
  }

  "return an nirecord with no tax years after 2015 when the FRY 2015" in {
    val niRecord = DesNIRecord(3, 4, 3, pre75ContributionCount = 0, Some(new LocalDate(2010, 4, 6)), List(
      taxYear(2010, true, false),
      taxYear(2011, false, false),
      taxYear(2012, false, true),
      taxYear(2013, true, false),
      taxYear(2014, true, false),
      taxYear(2015, false, true),
      taxYear(2016, false, true)
    ))

    val purged = niRecord.purge(finalRelevantStartYear = 2015)

    purged.numberOfQualifyingYears shouldBe 3
    purged.nonQualifyingYears shouldBe 3
    purged.nonQualifyingYearsPayable shouldBe 2
    purged.pre75ContributionCount shouldBe 0
    purged.dateOfEntry shouldBe Some(new LocalDate(2010, 4, 6))
    purged.niTaxYears shouldBe List(
      taxYear(2010, true, false),
      taxYear(2011, false, false),
      taxYear(2012, false, true),
      taxYear(2013, true, false),
      taxYear(2014, true, false),
      taxYear(2015, false, true)
    )
  }
}
