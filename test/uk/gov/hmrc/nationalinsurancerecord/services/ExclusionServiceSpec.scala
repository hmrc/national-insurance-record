/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesLiability

import java.time.LocalDate

class ExclusionServiceSpec extends NationalInsuranceRecordUnitSpec {
  // scalastyle:off magic.number

  val exampleNow = LocalDate.of(2017, 2, 16)
  val examplePensionDate = LocalDate.of(2022, 2, 2)

  private def exclusionServiceBuilder(
                                dateOfDeath: Option[LocalDate] = None,
                                liabilities: List[DesLiability] = List(),
                                manualCorrespondenceOnly: Boolean = false
                             ) = new DesExclusionService(dateOfDeath, liabilities, manualCorrespondenceOnly)

  "getExclusions" when {
    "there is no exclusions" must{
      "return an empty list" in {
        exclusionServiceBuilder(dateOfDeath = None).getExclusions shouldBe Nil
      }
    }

    "there is a date of death" must{
      "return a List(Dead)" in {
        exclusionServiceBuilder(dateOfDeath = Some(LocalDate.of(2000, 9, 13))).getExclusions shouldBe List(Exclusion.Dead)
      }
    }

    "the isle of man criteria is met" when {
      "there is no liabilities" must {
        "return no exclusions" in {
          exclusionServiceBuilder(liabilities = List()).getExclusions shouldBe Nil
        }
      }
      "there is some liabilities" must{
        "return List(IsleOfMan) if the list includes liability type 15" in {
          exclusionServiceBuilder(liabilities = List(DesLiability(Some(5)), DesLiability(Some(16)))).getExclusions shouldBe List(Exclusion.IsleOfMan)
        }
        "return no exclusions if the list does not include liability type 15" in {
          exclusionServiceBuilder(liabilities = List(DesLiability(Some(15)), DesLiability(Some(16)))).getExclusions shouldBe Nil
        }
      }
    }

    "there is manual correspondence only" must{
      "return List(ManualCorrespondenceIndicator)" in {
        exclusionServiceBuilder(manualCorrespondenceOnly = true).getExclusions shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }

    "there is not manual correspondence only" must{
      "return no exclusions" in {
        exclusionServiceBuilder(manualCorrespondenceOnly = false).getExclusions shouldBe Nil
      }
    }

    "all the exclusion criteria are met" must{
      "return a sorted list of Dead, MCI, IoM exclusions" in {
        exclusionServiceBuilder(
          dateOfDeath = Some(LocalDate.of(1999, 12, 31)),
          liabilities = List(DesLiability(Some(5)), DesLiability(Some(15)), DesLiability(Some(1))),
          manualCorrespondenceOnly = true
        ).getExclusions shouldBe List(
          Exclusion.Dead,
          Exclusion.ManualCorrespondenceIndicator,
          Exclusion.IsleOfMan
        )
      }
    }
  }

}
