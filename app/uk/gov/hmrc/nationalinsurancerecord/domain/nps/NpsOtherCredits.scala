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

package uk.gov.hmrc.nationalinsurancerecord.domain.nps

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class NpsOtherCredits(creditContributionType: Int, creditSourceType: Int, numberOfCredits: Int)

object NpsOtherCredits {
  val reads: Reads[NpsOtherCredits] = (
      (__ \ "cc_type").read[Int] and
      (__ \ "credit_source_type").read[Int] and
      (__ \ "no_of_credits_and_conts").read[Int]
    )(NpsOtherCredits.apply _)

  val writes: Writes[NpsOtherCredits] = (
      (__ \ "cc_type").write[Int] and
      (__ \ "credit_source_type").write[Int] and
      (__ \ "no_of_credits_and_conts").write[Int]
    )(unlift(NpsOtherCredits.unapply))

  implicit val format: Format[NpsOtherCredits] = Format(reads, writes)

}
