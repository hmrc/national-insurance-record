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


case class NpsSummary(
                     rreToConsider: Boolean = false,
                     dateOfDeath: Option[LocalDate] = None,
                     earningsIncludedUpTo: LocalDate,
                     dateOfBirth: LocalDate,
                     finalRelevantYear: Int
                     )

object NpsSummary {
  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))
  val writeIntFromBoolean: JsPath => OWrites[Boolean] = jsPath => jsPath.write[Int].contramap[Boolean] {
    case true => 1
    case _ => 0
  }

  val writes: Writes[NpsSummary] = (
    writeIntFromBoolean(__ \ "rre_to_consider") and
    (__ \ "date_of_death").write[Option[LocalDate]] and
    (__ \ "earnings_included_upto").write[LocalDate] and
    (__ \ "date_of_birth").write[LocalDate] and
    (__ \ "final_relevant_year").write[Int]
    )(unlift(NpsSummary.unapply))

  val reads: Reads[NpsSummary] = (
      readBooleanFromInt(__ \ "rre_to_consider") and
      (__ \ "date_of_death").readNullable[LocalDate] and
      (__ \ "earnings_included_upto").read[LocalDate] and
      (__ \ "date_of_birth").read[LocalDate] and
      (__ \ "final_relevant_year").read[Int]
    )(NpsSummary.apply _)

  implicit val formats: Format[NpsSummary] = Format(reads, writes)
}
