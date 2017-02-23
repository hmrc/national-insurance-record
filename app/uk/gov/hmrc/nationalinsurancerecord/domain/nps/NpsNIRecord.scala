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

package uk.gov.hmrc.nationalinsurancerecord.domain.nps

import org.joda.time.LocalDate
import play.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class NpsNIRecord(
                       numberOfQualifyingYears: Int = 0,
                       nonQualifyingYears: Int = 0,
                       nonQualifyingYearsPayable: Int = 0,
                       pre75ContributionCount: Int = 0,
                       dateOfEntry: LocalDate,
                       niTaxYears: List[NpsNITaxYear]
                      ) {

  def purge(fry: Int): NpsNIRecord = {
    val taxYears = niTaxYears.filter(_.taxYear <= fry)
    val purgedYears = niTaxYears.filter(_.taxYear > fry)
    if(purgedYears.nonEmpty) Logger.info(s"Purged years (FRY $fry): ${purgedYears.map(_.taxYear).mkString(",")}")

    this.copy(nonQualifyingYears =
      taxYears.count(!_.qualifying),
      nonQualifyingYearsPayable = taxYears.count(year => !year.qualifying && year.payable && !year.underInvestigation),
      niTaxYears = taxYears
    )
  }
}

object NpsNIRecord {
  implicit val reads: Reads[NpsNIRecord] = (
        (__ \ "number_of_qualifying_years").read[Int] and
        (__ \ "non_qualifying_years").read[Int] and
        (__ \ "non_qualifying_years_payable").read[Int] and
        (__ \ "pre_75_cc_count").read[Int] and
        (__ \ "date_of_entry").read[LocalDate] and
        (__ \ "npsLnitaxyr").read[List[NpsNITaxYear]]
    ) (NpsNIRecord.apply _)
}
