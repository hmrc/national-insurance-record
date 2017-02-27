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
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class NpsLiability(
                         liabilityType: Int,
                         startYear: Option[LocalDate],
                         endYear: Option[LocalDate])

object NpsLiability {
  implicit val reads: Reads[NpsLiability] = (
      (__ \ "liability_type").read[Int] and
      (__ \ "liability_type_start_date").readNullable[LocalDate] and
      (__ \ "liability_type_end_date").readNullable[LocalDate]
    )(NpsLiability.apply _)
}
