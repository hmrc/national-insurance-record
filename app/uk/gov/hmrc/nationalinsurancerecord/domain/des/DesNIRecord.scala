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
import play.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DesNIRecord(
                       numberOfQualifyingYears: Int = 0,
                       nonQualifyingYears: Int = 0,
                       nonQualifyingYearsPayable: Int = 0,
                       pre75ContributionCount: Int = 0,
                       dateOfEntry: Option[LocalDate],
                       niTaxYears: List[DesNITaxYear]
                      ) {

  def purge(finalRelevantStartYear: Int): DesNIRecord = {
    val taxYears = niTaxYears.filter(_.startTaxYear <= finalRelevantStartYear)
    val purgedYears = niTaxYears.filter(_.startTaxYear > finalRelevantStartYear)
    if(purgedYears.nonEmpty) Logger.info(s"Purged years (FRY $finalRelevantStartYear): ${purgedYears.map(_.startTaxYear).mkString(",")}")

    this.copy(
      nonQualifyingYears = taxYears.count(!_.qualifying),
      nonQualifyingYearsPayable = taxYears.count(year => !year.qualifying && year.payable && !year.underInvestigation),
      niTaxYears = taxYears
    )
  }
}

object DesNIRecord {
  val reads: Reads[DesNIRecord] = (
        (__ \ "numberOfQualifyingYears").read[Int] and
        (__ \ "nonQualifyingYears").read[Int] and
        (__ \ "nonQualifyingYearsPayable").read[Int] and
        (__ \ "pre75CcCount").read[Int] and
        (__ \ "dateOfEntry").readNullable[LocalDate] and
        (__ \ "taxYears").read[List[DesNITaxYear]]
    )(DesNIRecord.apply _)

  val writes: Writes[DesNIRecord] = (
    (__ \ "number_of_qualifying_years").write[Int] and
    (__ \ "nonQualifyingYears").write[Int] and
    (__ \ "nonQualifyingYearsPayable").write[Int] and
    (__ \ "pre75CcCount").write[Int] and
    (__ \ "dateOfEntry").writeNullable[LocalDate] and
    (__ \ "taxYears").write[List[DesNITaxYear]]
    )(unlift(DesNIRecord.unapply))

  implicit val format: Format[DesNIRecord] = Format(reads, writes)
}