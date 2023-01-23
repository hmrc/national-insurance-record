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

import play.api.libs.functional.syntax._
import uk.gov.hmrc.nationalinsurancerecord.util.DateFormats.localDateFormat
import play.api.libs.json._

import java.time.LocalDate


case class DesSummary(
                     rreToConsider: Boolean = false,
                     dateOfDeath: Option[LocalDate] = None,
                     earningsIncludedUpTo: Option[LocalDate],
                     dateOfBirth: Option[LocalDate],
                     finalRelevantYear: Option[Int]
                     )

object DesSummary {

  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))
  val writeIntFromBoolean: JsPath => OWrites[Boolean] = jsPath => jsPath.write[Int].contramap[Boolean] {
    case true => 1
    case _ => 0
  }

  val readBooleanwithDefault: JsPath => Reads[Boolean] = jsPath => jsPath.readNullable[Boolean].map(_.getOrElse(false))


  val writes: Writes[DesSummary] = (
    (__ \ "reducedRateElectionToConsider").write[Boolean] and
    (__ \ "dateOfDeath").write[Option[LocalDate]] and
    (__ \ "earningsIncludedUpto").write[Option[LocalDate]] and
    (__ \ "dateOfBirth").write[Option[LocalDate]] and
    (__ \ "finalRelevantYear").write[Option[Int]]
    )(unlift(DesSummary.unapply))

  val reads: Reads[DesSummary] = (
    readBooleanwithDefault(__ \ "reducedRateElectionToConsider") and
      (__ \ "dateOfDeath").readNullable[LocalDate] and
      (__ \ "earningsIncludedUpto").readNullable[LocalDate] and
      (__ \ "dateOfBirth").readNullable[LocalDate] and
      (__ \ "finalRelevantYear").readNullable[Int]
    )(DesSummary.apply _)

  implicit val formats: Format[DesSummary] = Format(reads, writes)
}
