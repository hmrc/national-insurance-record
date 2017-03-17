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
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain.nps.NpsLiability

class ExclusionServiceSpec extends NationalInsuranceRecordServiceSpec {
  // scalastyle:off magic.number

  val exampleNow = new LocalDate(2017, 2, 16)
  val examplePensionDate = new LocalDate(2022, 2, 2)

  private def exclusionServiceBuilder(
                                dateOfDeath: Option[LocalDate] = None,
                                reducedRateElection: Boolean = false,
                                liabilities: List[NpsLiability] = List(),
                                manualCorrespondenceOnly: Boolean = false
                             ) = new ExclusionService(dateOfDeath, reducedRateElection, liabilities, manualCorrespondenceOnly)

  "getExclusions" when {
    "there is no exclusions" should {
      "return an empty list" in {
        exclusionServiceBuilder(dateOfDeath = None).getExclusions shouldBe Nil
      }
    }

    "there is a date of death" should {
      "return a List(Dead)" in {
        exclusionServiceBuilder(dateOfDeath = Some(new LocalDate(2000, 9, 13))).getExclusions shouldBe List(Exclusion.Dead)
      }
    }

    "there is reduced rate election" should {
      "return a List(MarriwedWomensReducedRateElection" in {
        exclusionServiceBuilder(reducedRateElection = true).getExclusions shouldBe List(Exclusion.MarriedWomenReducedRateElection)
      }
    }

    "there is no reduced rate election" should {
      "return no exclusions" in {
        exclusionServiceBuilder(reducedRateElection = false).getExclusions shouldBe Nil
      }
    }

    "the isle of man criteria is met" when {
      "there is no liabilities" should  {
        "return no exclusions" in {
          exclusionServiceBuilder(liabilities = List()).getExclusions shouldBe Nil
        }
      }
      "there is some liabilities" should {
        "return List(IsleOfMan) if the list includes liability type 15" in {
          exclusionServiceBuilder(liabilities = List(NpsLiability(5), NpsLiability(16))).getExclusions shouldBe List(Exclusion.IsleOfMan)
        }
        "return no exclusions if the list does not include liability type 15" in {
          exclusionServiceBuilder(liabilities = List(NpsLiability(15), NpsLiability(16))).getExclusions shouldBe Nil
        }
      }
    }

    "there is manual correspondence only" should {
      "return List(ManualCorrespondenceIndicator)" in {
        exclusionServiceBuilder(manualCorrespondenceOnly = true).getExclusions shouldBe List(Exclusion.ManualCorrespondenceIndicator)
      }
    }

    "there is not manual correspondence only" should {
      "return no exclusions" in {
        exclusionServiceBuilder(manualCorrespondenceOnly = false).getExclusions shouldBe Nil
      }
    }

    "all the exclusion criteria are met" should {
      "return a sorted list of Dead, PostSPA, MWRRE" in {
        exclusionServiceBuilder(
          dateOfDeath = Some(new LocalDate(1999, 12, 31)),
          reducedRateElection = true,
          liabilities = List(NpsLiability(5), NpsLiability(15), NpsLiability(1)),
          manualCorrespondenceOnly = true
        ).getExclusions shouldBe List(
          Exclusion.Dead,
          Exclusion.ManualCorrespondenceIndicator,
          Exclusion.IsleOfMan,
          Exclusion.MarriedWomenReducedRateElection
        )
      }
    }
  }

}
