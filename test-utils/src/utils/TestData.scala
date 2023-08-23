/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import play.api.libs.json.Json
import uk.gov.hmrc.nationalinsurancerecord.domain.des._

import scala.io.Source

object TestData {
  val niRecordJson: String =
    Source
      .fromInputStream(getClass.getResourceAsStream("/json/niRecordDesProxyCacheTest.json"))
      .mkString

  val summaryJson: String =
    Source
      .fromInputStream(getClass.getResourceAsStream("/json/nisummaryDesTest.json"))
      .mkString

  val desLiabilitiesJson: String =
    """
      |{
      |    "liabilities": [
      |        {
      |            "awardAmount": 123.49,
      |            "liabilityOccurrenceNo": 89,
      |            "liabilityType": 17,
      |            "liabilityTypeEndDate": "2014-08-25",
      |            "liabilityTypeEndDateReason": "END DATE HELD",
      |            "liabilityTypeStartDate": "2014-08-25",
      |            "nino":"SK196234"
      |        },
      |        {
      |            "awardAmount": 456.54,
      |            "liabilityOccurrenceNo": 90,
      |            "liabilityType": 45,
      |            "liabilityTypeEndDate": "2018-08-25",
      |            "liabilityTypeEndDateReason": "END DATE HELD",
      |            "liabilityTypeStartDate": "2017-08-26",
      |            "nino":"SK196234"
      |        }
      |
      |    ]
      |}
  """.stripMargin

  val desSummary: DesSummary =
    Json.parse(summaryJson).as[DesSummary]

  val desNIRecord: DesNIRecord =
    Json.parse(niRecordJson).as[DesNIRecord]

  val desLiabilities: DesLiabilities =
    Json.parse(desLiabilitiesJson).as[DesLiabilities]

  val proxyCacheData: ProxyCacheData =
    ProxyCacheData(
      summary = desSummary,
      niRecord = desNIRecord,
      liabilities = desLiabilities
    )
}
