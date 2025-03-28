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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DesOtherCredits(creditContributionType: Option[Int], creditSourceType: Option[Int], numberOfCredits: Option[Int])

object DesOtherCredits {
  val reads: Reads[DesOtherCredits] = (
      (__ \ "ccType").readNullable[Int] and
      (__ \ "creditSourceType").readNullable[Int] and
      (__ \ "numberOfCredits").readNullable[Int]
    )(DesOtherCredits.apply)

  val writes: Writes[DesOtherCredits] = (
      (__ \ "ccType").writeNullable[Int] and
      (__ \ "creditSourceType").writeNullable[Int] and
      (__ \ "numberOfCredits").writeNullable[Int]
    )(o => Tuple.fromProductTyped(o))

  implicit val format: Format[DesOtherCredits] = Format(reads, writes)

}
