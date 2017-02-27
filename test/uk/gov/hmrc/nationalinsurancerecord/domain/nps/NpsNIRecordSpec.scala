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

package uk.gov.hmrc.nationalinsurancerecord.domain.nps

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class NpsNIRecordSpec extends UnitSpec{
  // scalastyle:off magic.number

  val niTaxYearJson = Json.parse(
    """
    | {
        | "years_to_fry": 1,
        | "non_qualifying_years": 13,
        | "date_of_entry": "1973-10-01",
        | "npsLniemply": [],
        | "pre_75_cc_count": 51,
        | "number_of_qualifying_years": 27,
        | "npsErrlist": {
          |   "count": 0,
          | "mgt_check": 0,
          | "commit_status": 2,
          | "npsErritem": [],
          | "bfm_return_code": 0,
          | "data_not_found": 0
          |},
        |"non_qualifying_years_payable": 0,
        |  "npsLnitaxyr": [
        | {
          | "class_three_payable_by_penalty": null,
          | "class_two_outstanding_weeks": null,
          | "class_two_payable": null,
          | "qualifying": 1,
          | "under_investigation_flag": 0,
          | "class_two_payable_by": null,
          | "co_class_one_paid": null,
          | "class_two_payable_by_penalty": null,
          | "co_primary_paid_earnings": null,
          | "payable": 0,
          | "rattd_tax_year": 2012,
          | "ni_earnings": null,
          | "amount_needed": null,
          | "primary_paid_earnings": "21750.0000",
          | "class_three_payable": null,
          | "ni_earnings_employed": "1698.9600",
          | "npsLothcred": [
          |   {
          |      "credit_source_type": 2,
          |      "cc_type": 23,
          |      "no_of_credits_and_conts": 4
          |   }
          | ],
          | "ni_earnings_self_employed": null,
          | "class_three_payable_by": null,
          | "ni_earnings_voluntary": null
          |},
        |{
          | "class_three_payable_by_penalty": "2023-04-05",
          | "class_two_outstanding_weeks": null,
          | "class_two_payable": null,
          | "qualifying": 0,
          | "under_investigation_flag": 1,
          | "class_two_payable_by": null,
          | "co_class_one_paid": null,
          | "class_two_payable_by_penalty": null,
          | "co_primary_paid_earnings": null,
          | "payable": 1,
          | "rattd_tax_year": 2013,
          | "ni_earnings": null,
          | "amount_needed": null,
          | "primary_paid_earnings": null,
          | "class_three_payable": 722.80,
          | "ni_earnings_employed": null,
          | "npsLothcred": [],
          | "ni_earnings_self_employed": "52",
          | "class_three_payable_by": "2019-04-05",
          | "ni_earnings_voluntary": null
          |}
        |],
        | "nino": "<NINO>"
        |}
    """.stripMargin)

    val niRecord = niTaxYearJson.as[NpsNIRecord]
    "NIRecord" should {
      "parse number of Qualifying year correctly" in {
        niRecord.numberOfQualifyingYears shouldBe 27
      }
      "parse number of non-qualifying year correctly" in {
        niRecord.nonQualifyingYears shouldBe 13
      }
      "parse number of non qualifying payable years correctly" in {
        niRecord.nonQualifyingYearsPayable shouldBe 0
      }
      "parse number pre-75 year contribution count correctly" in {
        niRecord.pre75ContributionCount shouldBe 51
      }
      "parse date of entry correctly" in {
        niRecord.dateOfEntry shouldBe new LocalDate(1973,10,1)
      }
      "parse qualifying status of tax year correctly" in {
        niRecord.niTaxYears.head.qualifying shouldBe true
        niRecord.niTaxYears(1).qualifying shouldBe false
      }
      "parse underInvestigation status of tax year correctly" in {
        niRecord.niTaxYears.head.underInvestigation shouldBe false
        niRecord.niTaxYears(1).underInvestigation shouldBe true
      }
      "parse payable status of tax year correctly" in {
        niRecord.niTaxYears.head.payable shouldBe false
        niRecord.niTaxYears(1).payable shouldBe true
      }
      "parse class three payable amount for qualifying and non-qualifying tax year correctly" in {
        niRecord.niTaxYears.head.classThreePayable shouldBe 0
        niRecord.niTaxYears(1).classThreePayable shouldBe 722.80
      }
      "parse class three payable by date correctly" in {
        niRecord.niTaxYears.head.classThreePayableBy shouldBe None
        niRecord.niTaxYears(1).classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
      }
      "parse class three payable by penalty date correctly" in {
        niRecord.niTaxYears.head.classThreePayableByPenalty shouldBe None
        niRecord.niTaxYears(1).classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
      }
      "parse niEarningsEmployed (ClassOneContribution) correctly" in {
        niRecord.niTaxYears.head.classOneContribution shouldBe 1698.9600
        niRecord.niTaxYears(1).classOneContribution shouldBe 0
      }
      "parse niEarningsSelfEmployed (ClassTwoCredits) correctly" in {
        niRecord.niTaxYears.head.classTwoCredits shouldBe 0
        niRecord.niTaxYears(1).classTwoCredits shouldBe 52
      }
      "parse otherCredits values correctly" in {
        niRecord.niTaxYears.head.otherCredits.head.creditSourceType shouldBe 2
        niRecord.niTaxYears.head.otherCredits.head.creditContributionType shouldBe 23
        niRecord.niTaxYears.head.otherCredits.head.numberOfCredits shouldBe 4

        niRecord.niTaxYears(1).otherCredits shouldBe List()
      }

    }

  def taxYear(year: Int, qualifying: Boolean, payable: Boolean): NpsNITaxYear = NpsNITaxYear(year, qualifying, false, payable, 0,None,None,0,0,0,List())

  "purge" should {
    "return an nirecord with no tax years after 2014 when the FRY 2014" in {
      val niRecord = NpsNIRecord(5, 2, 2, pre75ContributionCount = 0, new LocalDate(2010, 4, 6), List(
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
      purged.dateOfEntry shouldBe new LocalDate(2010, 4, 6)
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
    val niRecord = NpsNIRecord(3, 4, 3, pre75ContributionCount = 0, new LocalDate(2010, 4, 6), List(
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
    purged.dateOfEntry shouldBe new LocalDate(2010, 4, 6)
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
