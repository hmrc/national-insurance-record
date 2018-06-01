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

class DesNIRecordSpec extends UnitSpec {

  val testRecord: String = Source.fromInputStream(getClass.getResourceAsStream("/json/nirecordDesTest.json")).mkString

  "reading DesNIRecord " should {

    "Parse DesNiREcord correctly" in {
      val niRecord = Json.parse(testRecord).as[DesNIRecord]
      niRecord.numberOfQualifyingYears shouldBe 37
      niRecord.nonQualifyingYears shouldBe 2
      niRecord.nonQualifyingYearsPayable shouldBe 2
      niRecord.pre75ContributionCount shouldBe 0
      niRecord.dateOfEntry shouldBe Some(new LocalDate(1986, 9, 8))
      niRecord.niTaxYears.head.qualifying shouldBe true
      niRecord.niTaxYears(1).qualifying shouldBe true
      niRecord.niTaxYears.head.underInvestigation shouldBe false
      niRecord.niTaxYears(1).underInvestigation shouldBe false
      niRecord.niTaxYears.head.payable shouldBe false
      niRecord.niTaxYears(1).payable shouldBe false
      niRecord.niTaxYears.head.classThreePayable shouldBe 0
      niRecord.niTaxYears(30).classThreePayable shouldBe 733.20
      niRecord.niTaxYears(30).classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
      niRecord.niTaxYears.head.classThreePayableByPenalty shouldBe None
      niRecord.niTaxYears(30).classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
      niRecord.niTaxYears.head.classOneContribution shouldBe 416.99
      niRecord.niTaxYears(1).classOneContribution shouldBe 466.85
      niRecord.niTaxYears.head.classTwoCredits shouldBe 0
//      niRecord.niTaxYears(1).classTwoCredits shouldBe 52
      niRecord.niTaxYears(17).otherCredits.head.creditSourceType shouldBe Some(47)
      niRecord.niTaxYears(17).otherCredits.head.creditContributionType shouldBe Some(24)
      niRecord.niTaxYears(17).otherCredits.head.numberOfCredits shouldBe Some(53)
      niRecord.niTaxYears(17).otherCredits shouldBe List(DesOtherCredits(Some(24),Some(47),Some(53)))
    }
  }
}
