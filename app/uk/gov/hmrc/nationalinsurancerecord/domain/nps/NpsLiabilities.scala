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

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class NpsLiabilities(liabilities: List[NpsLiability])

object NpsLiabilities {
  val reads: Reads[NpsLiabilities] = {
    (__ \ "npsLcdo004d").read[List[NpsLiability]].map(NpsLiabilities.apply)
  }
  val writes: Writes[NpsLiabilities] = {
    (__ \ "npsLcdo004d").write[List[NpsLiability]].contramap(_.liabilities)
  }
  implicit val formats: Format[NpsLiabilities] = Format(reads, writes)
}
