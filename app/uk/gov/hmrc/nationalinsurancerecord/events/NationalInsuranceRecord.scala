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

object NationalInsuranceRecord {
  def apply(nino: Nino, qualifyingYears: Int, qualifyingYearsPriorTo1975: Int,
            numberOfGaps: Int, numberOfGapsPayable: Int, dateOfEntry: Option[LocalDate],
            homeResponsibilitiesProtection: Boolean, earningsIncludedUpTo: LocalDate, numberOfTaxYears: Int)(implicit hc: HeaderCarrier): NationalInsuranceRecord =
    new NationalInsuranceRecord(nino: Nino, qualifyingYears, qualifyingYearsPriorTo1975, numberOfGaps,
      numberOfGapsPayable, dateOfEntry, homeResponsibilitiesProtection: Boolean, earningsIncludedUpTo: LocalDate,
      numberOfTaxYears: Int)
}

class NationalInsuranceRecord(nino: Nino, qualifyingYears: Int, qualifyingYearsPriorTo1975: Int,
                              numberOfGaps: Int, numberOfGapsPayable: Int, dateOfEntry: Option[LocalDate],
                              homeResponsibilitiesProtection: Boolean, earningsIncludedUpTo: LocalDate, numberOfTaxYears: Int)(implicit hc: HeaderCarrier)
  extends BusinessEvent("NationalInsuranceRecord", nino,
    Map(
      "qualifyingYears" -> qualifyingYears.toString,
      "qualifyingYearsPriorTo1975" -> qualifyingYearsPriorTo1975.toString,
      "numberOfGaps" -> numberOfGaps.toString,
      "numberOfGapsPayable" -> numberOfGapsPayable.toString,
      "dateOfEntry" -> dateOfEntry.fold("")(_.toString),
      "homeResponsibilitiesProtection" -> homeResponsibilitiesProtection.toString,
      "earningsIncludedUpTo" -> earningsIncludedUpTo.toString,
      "numberOfTaxYears" -> numberOfTaxYears.toString
    )

  )
