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

package uk.gov.hmrc.nationalinsurancerecord.events

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier

object NationalInsuranceRecord{
  def apply(nino: Nino, qualifyingYears: Int, qualifyingYearsPriorTo1975: Int,
            numberOfGaps: Int, numberOfGapsPayable: Int, dateOfEntry: LocalDate,
            homeResponsibilitiesProtection: Boolean)(implicit hc: HeaderCarrier): NationalInsuranceRecord =
    new NationalInsuranceRecord(nino: Nino, qualifyingYears: Int,
                                           qualifyingYearsPriorTo1975: Int,
                                           numberOfGaps: Int,
                                           numberOfGapsPayable: Int,
                                           dateOfEntry: LocalDate,
                                           homeResponsibilitiesProtection: Boolean
                                          )
}

class NationalInsuranceRecord(nino: Nino, qualifyingYears: Int, qualifyingYearsPriorTo1975: Int,
                              numberOfGaps: Int, numberOfGapsPayable: Int, dateOfEntry: LocalDate,
                              homeResponsibilitiesProtection: Boolean) (implicit hc: HeaderCarrier)
  extends BusinessEvent("NationalInsuranceRecordSummary", nino,
    Map(
      "qualifyingYears" -> qualifyingYears.toString,
      "qualifyingYearsPriorTo1975" -> qualifyingYearsPriorTo1975.toString,
      "numberOfGaps" -> numberOfGaps.toString,
      "numberOfGapsPayable" -> numberOfGapsPayable.toString,
      "dateOfEntry" -> dateOfEntry.toString,
      "homeResponsibilitiesProtection" -> homeResponsibilitiesProtection.toString
    )

  )
