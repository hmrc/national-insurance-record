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

package uk.gov.hmrc.nationalinsurancerecord.domain.nps

import org.joda.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec
import play.api.libs.json.{JsValue, Json}

class NpsSummarySpec extends UnitSpec {
  // scalastyle:off magic.number

  val summary: JsValue = Json.parse(
    """
      |{
      | "contracted_out_flag": 1,
      | "sensitive_flag": 0,
      | "spa_date": "2017-11-21",
      | "final_relevant_year": 2016,
      | "account_not_maintained_flag": null,
      | "npsPenfor": {
      | "forecast_amount": 130.29,
      | "nsp_max": 151.25,
      | "qualifying_years_at_spa": 30,
      | "forecast_amount_2016": 125.97
      | },
      | "pension_share_order_coeg": 0,
      | "date_of_death": null,
      | "sex": "M",
      | "npsSpnam": {
      | "nsp_entitlement": 118.24,
      | "ap_amount": 0.0,
      | "npsAmnbpr16": {
      |   "main_component": 116.68,
      |   "rebate_derived_amount": 0.0
      |  },
      | "npsAmnapr16": {
      |   "ltb_post97_ap_cash_value": 0.0,
      |   "ltb_cat_a_cash_value": 104.36,
      |   "ltb_post88_cod_cash_value": null,
      |   "ltb_pre97_ap_cash_value": 7.06,
      |   "ltb_pre88_cod_cash_value": null,
      |   "grb_cash": 0.0,
      |   "ltb_pst88_gmp_cash_value": null,
      |   "pre88_gmp": null,
      |   "ltb_post02_ap_cash_value": 6.82
      |},
      | "protected_payment_2016": 0.0,
      | "starting_amount": 118.24
      |},
      |"npsErrlist": {
      | "count": 0,
      | "mgt_check": 0,
      | "commit_status": 2,
      | "npsErritem": [],
      | "bfm_return_code": 0,
      | "data_not_found": 0
      |},
      |"date_of_birth": "1952-11-21",
      |  "nsp_qualifying_years": 27,
      |  "country_code": 1,
      | "nsp_requisite_years": 35,
      | "minimum_qualifying_period": 1,
      | "address_postcode": "EC4 5YY",
      | "rre_to_consider": 0,
      | "pension_share_order_serps": 0,
      | "nino": "<NINO>",
      | "earnings_included_upto": "2014-04-05"
      |}
    """.stripMargin)

  val niSummary: NpsSummary = summary.as[NpsSummary]
  "NISummary" should {
    "parse rre to consider correctly" in {
      niSummary.rreToConsider shouldBe false
    }
    "parse date of death correctly" in {
      niSummary.dateOfDeath shouldBe None
    }
    "parse date of birth correctly" in {
      niSummary.dateOfBirth shouldBe new LocalDate(1952, 11, 21)
    }
    "parse earnings Included upto correctly" in {
      niSummary.earningsIncludedUpTo shouldBe new LocalDate(2014, 4, 5)
    }
    "parse final relevant year correctly" in {
      niSummary.finalRelevantYear shouldBe 2016
    }
  }

  "NpsSummary" should {
    val expected = new NpsSummary(
      rreToConsider = true,
      dateOfDeath = Some(new LocalDate(2017, 1, 1)),
      earningsIncludedUpTo = new LocalDate(2016, 12, 31),
      dateOfBirth = new LocalDate(1968, 2, 24),
      finalRelevantYear = 2016
    )

    val json = Json.toJson[NpsSummary](expected)
    val actual = Json.fromJson[NpsSummary](json)

    "convert to JSON" in {
      val rreToConsider = (json \ "rre_to_consider").asOpt[Int]
      rreToConsider.isDefined shouldBe true
      rreToConsider.get shouldBe 1
    }
    "convert from JSON" in {
      actual.isSuccess shouldBe true
    }
    "convert the same value to and from JSON" in {
      actual.get shouldBe expected
      actual.get.rreToConsider shouldBe true
    }
  }

}
