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
import play.api.libs.functional.syntax._
import play.api.libs.json._


case class DesSummary(
                     rreToConsider: Boolean = false,
                     dateOfDeath: Option[LocalDate] = None,
                     earningsIncludedUpTo: LocalDate,
                     dateOfBirth: LocalDate,
                     finalRelevantYear: Int
                     )

object DesSummary {
  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))
  val writeIntFromBoolean: JsPath => OWrites[Boolean] = jsPath => jsPath.write[Int].contramap[Boolean] {
    case true => 1
    case _ => 0
  }

  val writes: Writes[DesSummary] = (
    (__ \ "reducedRateElectionToConsider").write[Boolean] and
    (__ \ "dateOfDeath").write[Option[LocalDate]] and
    (__ \ "earningsIncludedUpto").write[LocalDate] and
    (__ \ "dateOfBirth").write[LocalDate] and
    (__ \ "finalRelevantYear").write[Int]
    )(unlift(DesSummary.unapply))

  val reads: Reads[DesSummary] = (
      readBooleanFromInt(__ \ "reducedRateElectionToConsider") and
      (__ \ "dateOfDeath").readNullable[LocalDate] and
      (__ \ "earningsIncludedUpto").read[LocalDate] and
      (__ \ "dateOfBirth").read[LocalDate] and
      (__ \ "finalRelevantYear").read[Int]
    )(DesSummary.apply _)

  implicit val formats: Format[DesSummary] = Format(reads, writes)
}
