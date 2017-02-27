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
import uk.gov.hmrc.play.test.UnitSpec

class NpsLiabilitySpec extends UnitSpec{
  // scalastyle:off magic.number

  "NPS Liability" should {

    "return liability type and dates correctly" in {
      NpsLiability(13, Some(new LocalDate(2015,3,8)), Some(new LocalDate(2015,4,4))).liabilityType shouldBe 13
      NpsLiability(13, Some(new LocalDate(2015,3,8)), Some(new LocalDate(2015,4,4))).startYear shouldBe Some(new LocalDate(2015,3,8))
      NpsLiability(13, Some(new LocalDate(2015,3,8)), Some(new LocalDate(2015,4,4))).endYear shouldBe Some(new LocalDate(2015,4,4))
    }
  }
}
