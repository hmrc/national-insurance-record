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

import uk.gov.hmrc.play.test.UnitSpec
import play.api.libs.json.Json

class NpsLiabilitySpec extends UnitSpec{
  // scalastyle:off magic.number

  "NPS Liability" should {

    "return liability type and dates correctly" in {
      NpsLiability(13).liabilityType shouldBe 13
    }

    "parse Nps response liability type correctly" in {
      Json.parse(
        """
           |{
           |    "liability_type_end_date": "2000-02-17",
           |    "liability_occurrence_no": 1,
           |    "liability_type_start_date": "1984-02-20",
           |    "liability_type_end_date_reason": "END DATE HELD",
           |    "liability_type": 16,
           |    "award_amount": null
           |}
         """.stripMargin).as[NpsLiability].liabilityType shouldBe 16
    }
  }
}
