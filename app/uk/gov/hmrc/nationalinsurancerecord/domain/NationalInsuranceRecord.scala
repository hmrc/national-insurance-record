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

package uk.gov.hmrc.nationalinsurancerecord.domain

import play.api.libs.functional.syntax._
import uk.gov.hmrc.nationalinsurancerecord.util.DateFormats.localDateFormat
import play.api.libs.json.{Json, Writes, _}

import java.time.LocalDate

case class NationalInsuranceRecord(
                                    qualifyingYears: Int,
                                    qualifyingYearsPriorTo1975: Int,
                                    numberOfGaps: Int,
                                    numberOfGapsPayable: Int,
                                    dateOfEntry: Option[LocalDate],
                                    homeResponsibilitiesProtection: Boolean,
                                    earningsIncludedUpTo: LocalDate,
                                    taxYears: List[NationalInsuranceTaxYear],
                                    reducedRateElection:Boolean
                                  )

object NationalInsuranceRecord {
  implicit val reads: Reads[NationalInsuranceRecord] = Json.reads[NationalInsuranceRecord]
  implicit val writes: Writes[NationalInsuranceRecord] = (
    (JsPath \ "qualifyingYears").write[Int] and
    (JsPath \ "qualifyingYearsPriorTo1975").write[Int] and
    (JsPath \ "numberOfGaps").write[Int] and
    (JsPath \ "numberOfGapsPayable").write[Int] and
    (JsPath \ "dateOfEntry").writeNullable[LocalDate] and
    (JsPath \ "homeResponsibilitiesProtection").write[Boolean] and
    (JsPath \ "earningsIncludedUpTo").write[LocalDate] and
    (JsPath \ "reducedRateElection").write[Boolean]
    )((ni: NationalInsuranceRecord) => (
      ni.qualifyingYears,
      ni.qualifyingYearsPriorTo1975,
      ni.numberOfGaps,
      ni.numberOfGapsPayable,
      ni.dateOfEntry,
      ni.homeResponsibilitiesProtection,
      ni.earningsIncludedUpTo,
      ni.reducedRateElection
    ))
}
