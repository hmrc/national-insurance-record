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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import java.time.LocalDate
import scala.io.Source

object ProxyCacheTestData {
  val niRecord: String =
    Source
      .fromInputStream(getClass.getResourceAsStream("/json/niRecordDesProxyCacheTest.json"))
      .mkString

  val summary: String =
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

  val desSummary: DesSummary = DesSummary(
    rreToConsider = true,
    dateOfDeath = Some(LocalDate.parse("2014-08-25")),
    earningsIncludedUpTo = Some(LocalDate.parse("2014-01-01")),
    dateOfBirth = Some(LocalDate.parse("2014-08-25")),
    finalRelevantYear = Some(2014)
  )

  val desNIRecord: DesNIRecord = DesNIRecord(
    numberOfQualifyingYears = 37,
    nonQualifyingYears = 2,
    nonQualifyingYearsPayable = 2,
    dateOfEntry = Some(LocalDate.parse("1986-09-08")),
    niTaxYears = List(
      DesNITaxYear(
        startTaxYear = 1986,
        qualifying = true,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 416.99,
        otherCredits = List(
          DesOtherCredits(None, None, None)
        )
      ),
      DesNITaxYear(
        startTaxYear = 1987,
        qualifying = true,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 466.85,
        otherCredits = List(
          DesOtherCredits(None, None, None)
        )
      ),
      DesNITaxYear(
        startTaxYear = 1988,
        qualifying = true,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 530.07,
        otherCredits = List(
          DesOtherCredits(None, None, None)
        )
      )
    )
  )

  val desLiabilities: DesLiabilities = DesLiabilities(
    liabilities = List(
      DesLiability(Some(17)),
      DesLiability(Some(45))
    )
  )
}
