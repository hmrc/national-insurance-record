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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

import scala.io.Source

class DesSummarySpec extends UnitSpec {
  val testCompletedRecord: String = Source.fromInputStream(getClass.getResourceAsStream("/json/nisummaryDesTest.json")).mkString
  val testEmptyRecord: String = Source.fromInputStream(getClass.getResourceAsStream("/json/nisummaryEmptyDesTest.json")).mkString

  "reading DesSummary" should {

    "parse empty DesSummary correctly" in {
      val niSummary = Json.parse(testEmptyRecord).as[DesSummary]
      niSummary.rreToConsider shouldBe false
      niSummary.dateOfDeath shouldBe None
      niSummary.earningsIncludedUpTo shouldBe None
      niSummary.dateOfBirth shouldBe None
      niSummary.finalRelevantYear shouldBe None
    }

    "parse full DesSummary correctly" in {
      val niSummary = Json.parse(testCompletedRecord).as[DesSummary]
      niSummary.rreToConsider shouldBe true
      niSummary.dateOfDeath shouldBe Some(LocalDate.parse("2014-08-25"))
      niSummary.earningsIncludedUpTo shouldBe Some(LocalDate.parse("2014-01-01"))
      niSummary.dateOfBirth shouldBe Some(LocalDate.parse("2014-08-25"))
      niSummary.finalRelevantYear shouldBe Some(2014)
    }
  }
}
