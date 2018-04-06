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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DesOtherCredits(creditContributionType: Int, creditSourceType: Int, numberOfCredits: Int)

object DesOtherCredits {
  val reads: Reads[DesOtherCredits] = (
      (__ \ "ccType").read[Int] and
      (__ \ "creditSourceType").read[Int] and
      (__ \ "numberOfCredits").read[Int]
    )(DesOtherCredits.apply _)

  val writes: Writes[DesOtherCredits] = (
      (__ \ "cc_type").write[Int] and
      (__ \ "credit_source_type").write[Int] and
      (__ \ "no_of_credits_and_conts").write[Int]
    )(unlift(DesOtherCredits.unapply))

  implicit val format: Format[DesOtherCredits] = Format(reads, writes)

}
