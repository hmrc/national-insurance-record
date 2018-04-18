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

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

import scala.io.Source

class DesNIRecordSpec extends UnitSpec {

  val testRecord = Source.fromInputStream(getClass().getResourceAsStream("/json/nirecordDesTest.json")).mkString

  "reading DesNIRecord " should {

    "Parse DesNiREcord correctly" in {
      val niRecord = Json.parse(testRecord).as[DesNIRecord]
      niRecord.numberOfQualifyingYears shouldBe 37
    }
  }
}
