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

package uk.gov.hmrc.nationalinsurancerecord.events

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

object NationalInsuranceTaxYear{
  def apply(nino: Nino, taxYear: String,
            qualifying: Boolean,
            classOneContributions: BigDecimal,
            classTwoCredits: Int,
            classThreeCredits: Int,
            otherCredits: Int,
            classThreePayable: BigDecimal,
            classThreePayableBy: Option[LocalDate],
            classThreePayableByPenalty: Option[LocalDate],
            payable: Boolean,
            underInvestigation: Boolean)(implicit hc: HeaderCarrier): NationalInsuranceTaxYear =
    new NationalInsuranceTaxYear(nino: Nino, taxYear: String,
      qualifying: Boolean,
      classOneContributions: BigDecimal,
      classTwoCredits: Int,
      classThreeCredits: Int,
      otherCredits: Int,
      classThreePayable: BigDecimal,
      classThreePayableBy: Option[LocalDate],
      classThreePayableByPenalty: Option[LocalDate],
      payable: Boolean,
      underInvestigation: Boolean
    )
}

class NationalInsuranceTaxYear(nino: Nino, taxYear: String,
                               qualifying: Boolean,
                               classOneContributions: BigDecimal,
                               classTwoCredits: Int,
                               classThreeCredits: Int,
                               otherCredits: Int,
                               classThreePayable: BigDecimal,
                               classThreePayableBy: Option[LocalDate],
                               classThreePayableByPenalty: Option[LocalDate],
                               payable: Boolean,
                               underInvestigation: Boolean) (implicit hc: HeaderCarrier)
  extends BusinessEvent("NationalInsuranceTaxYear", nino,
    Map(
      "taxYear" -> taxYear.toString,
      "qualifying" -> qualifying.toString,
      "classOneContributions" -> classOneContributions.toString,
      "classTwoCredits" -> classTwoCredits.toString,
      "classThreeCredits" -> classThreeCredits.toString,
      "otherCredits" -> otherCredits.toString,
      "classThreePayable" -> classThreePayable.toString,
      "classThreePayableBy" -> classThreePayableBy.map(_.toString).getOrElse(""),
      "classThreePayableByPenalty" -> classThreePayableByPenalty.map(_.toString).getOrElse(""),
      "payable" -> payable.toString,
      "underInvestigation" -> underInvestigation.toString
    )

  )
