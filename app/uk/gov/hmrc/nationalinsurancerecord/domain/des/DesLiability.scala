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

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DesLiability(liabilityType: Int)

object DesLiability {
  val reads: Reads[DesLiability] = (__ \ "liabilityType").read[Int].map(DesLiability.apply)
  val writes: Writes[DesLiability] = (__ \ "liability_type").write[Int].contramap(_.liabilityType)
  implicit val formats: Format[DesLiability] = Format(reads, writes)
}


object LiabilityType {
  final val ISLE_OF_MAN = 5
}
